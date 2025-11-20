package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public class Test4Steps {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public Test4Steps(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    private void debug(String msg) {
        System.out.println("[DEBUG] " + msg);
    }

    /**
     * Generic retry helper for operations that may throw StaleElementReferenceException.
     */
    private <T> T retryStale(Supplier<T> action) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                attempts++;
                debug("retryStale attempt " + attempts + " due to stale element");
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Operation failed repeatedly due to stale elements");
    }

    private WebElement findVisible(By locator) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            } catch (StaleElementReferenceException e) {
                attempts++;
                debug("Stale element on findVisible attempt " + attempts + ", retrying...");
            }
        }
        throw new RuntimeException("Element not visible after retries: " + locator);
    }

    private List<WebElement> findVisibleAll(By locator) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
            } catch (StaleElementReferenceException e) {
                attempts++;
                debug("Stale element on findVisibleAll attempt " + attempts + ", retrying...");
            }
        }
        throw new RuntimeException("Elements not visible after retries: " + locator);
    }

    private void safeClick(WebElement el) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                wait.until(ExpectedConditions.elementToBeClickable(el)).click();
                return;
            } catch (ElementClickInterceptedException | TimeoutException | StaleElementReferenceException e) {
                attempts++;
                debug("safeClick attempt " + attempts + " failed, retrying...");
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Failed to click element after 3 attempts");
    }

    private void waitForOverlayToDisappear() {
        By overlay = By.cssSelector(".loading-overlay, .spinner, .blockUI, .modal-backdrop.show, .overlay, .page-loader");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.invisibilityOfElementLocated(overlay));
        } catch (TimeoutException ignored) {
        }
    }

    private void closeUnexpectedTabsExcept(String mainHandle) {
        try {
            Set<String> handles = driver.getWindowHandles();
            for (String h : handles) {
                if (!h.equals(mainHandle)) {
                    driver.switchTo().window(h);
                    driver.close();
                }
            }
            driver.switchTo().window(mainHandle);
        } catch (Exception ignored) {
        }
    }

    private ExpectedCondition<Boolean> elementIsEnabled(WebElement el) {
        return d -> {
            try {
                return el != null && el.isDisplayed() && el.isEnabled()
                        && !"true".equalsIgnoreCase(el.getAttribute("aria-disabled"))
                        && (el.getAttribute("disabled") == null)
                        && !(String.valueOf(el.getAttribute("class")).toLowerCase().contains("disabled"));
            } catch (StaleElementReferenceException e) {
                return false;
            }
        };
    }

    public void chooseCategory(String searchTerm) {
        debug("Choosing category: " + searchTerm);
        WebElement categoryInput = findVisible(By.id("fetch_service"));
        safeClick(categoryInput);
        categoryInput.clear();
        categoryInput.sendKeys(searchTerm);

        // Read aria-controls from a fresh element (in case it was re-rendered)
        String listId = retryStale(() -> {
            WebElement fresh = driver.findElement(By.id("fetch_service"));
            String v = fresh.getAttribute("aria-controls");
            return v;
        });
        if (listId == null || listId.isBlank()) listId = "autoComplete_list_1";

        // Wait for dropdown menu to appear (re-finding each time)
        wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(By.id(listId))));

        By options = By.cssSelector("#" + listId + " [role='option']");
        List<WebElement> items = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(options));
        if (items.isEmpty()) throw new TimeoutException("No autocomplete options found");

        safeClick(items.get(0));

        // Wait until the input's value updates — re-find element inside lambda to avoid stale refs
        wait.until(driver -> {
            try {
                WebElement fresh = driver.findElement(By.id("fetch_service"));
                String val = fresh.getAttribute("value");
                return val != null && val.trim().length() > 1 && !val.trim().equalsIgnoreCase(searchTerm.trim());
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });

        String chosen = retryStale(() -> driver.findElement(By.id("fetch_service")).getAttribute("value"));
        debug("Category chosen: " + chosen);
    }

    public void chooseFirstSubcategory() {
        debug("Choosing first subcategory");
        By labelSelector = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = findVisibleAll(labelSelector);
        if (labels.isEmpty()) throw new RuntimeException("No subcategories found.");
        safeClick(labels.get(0));
        String targetId = labels.get(0).getAttribute("for");
        if (targetId != null && !targetId.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeSelected(By.id(targetId)));
        }
        debug("Subcategory chosen");
    }

    private void waitForSubcategoryChecked() {
        debug("Waiting for subcategory check");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[name='type_of_service']:checked")));
    }

    private void waitForDateStep() {
        debug("Waiting for date input step");
        WebElement dateInput = findVisible(By.cssSelector("input[name='start_date']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", dateInput);
    }

    public void setDateByJS(String ddMMyyyyWithDashes) {
        debug("Setting date by JS: " + ddMMyyyyWithDashes);
        WebElement dateInput = findVisible(By.cssSelector("input[name='start_date']"));
        String js = "const el=arguments[0],v=arguments[1];el.value=v;el.setAttribute('value',v);" +
                "['input','change','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true})));";
        ((JavascriptExecutor) driver).executeScript(js, dateInput, ddMMyyyyWithDashes);

        // Re-find input on each check
        wait.until(drv -> {
            try {
                WebElement fresh = drv.findElement(By.cssSelector("input[name='start_date']"));
                String v = fresh.getAttribute("value");
                return v != null && v.trim().equals(ddMMyyyyWithDashes);
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });
        debug("Date set");
    }

    public void pickTimeByText(String timeTextDesired) {
        debug("Open time dropdown");
        WebElement trigger = findVisible(By.id("customDropdownButton"));
        safeClick(trigger);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".dropdown-menu.show, [role='menu'].show, .time-menu.show, .time-list.show")));

        List<WebElement> selects = findVisibleAll(By.xpath("//button[normalize-space()='Select']"));
        if (selects.isEmpty()) throw new TimeoutException("No time rows available");

        String desired = normalizeTime(timeTextDesired);
        String desiredNo0 = desired.replaceAll("\\b0([1-9]:)", "$1");

        for (WebElement btn : selects) {
            WebElement row = btn;
            try {
                row = btn.findElement(By.xpath("./ancestor::*[self::tr or self::li or self::div][1]"));
            } catch (Exception ignored) {}
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", row);
            String txt = normalizeTime(row.getText());
            if (txt.contains(desired) || txt.contains(desiredNo0)) {
                safeClick(btn);
                debug("Time selected: " + txt);
                return;
            }
        }
        safeClick(selects.get(0));
        debug("Fallback: selected first time option");
    }

    public void setTimeByJS(String timeText) {
        debug("Setting time by JS: " + timeText);
        WebElement timeInput = findVisible(By.cssSelector("input[name='start_time']"));
        String js = "const el=arguments[0],v=arguments[1];el.value=v;el.setAttribute('value',v);" +
                "['input','change','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true})));";
        ((JavascriptExecutor) driver).executeScript(js, timeInput, timeText);

        wait.until(drv -> {
            try {
                WebElement fresh = drv.findElement(By.cssSelector("input[name='start_time']"));
                String v = fresh.getAttribute("value");
                return v != null && v.trim().equals(timeText);
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });
    }

    private String normalizeTime(String s) {
        if (s == null) return "";
        String t = s.replace('\u00A0',' ').replaceAll("[\\s\\n\\r]+"," ").trim();
        t = t.replaceAll("(?i)am","AM").replaceAll("(?i)pm","PM");
        return t;
    }

    private void closeOpenMenusIfAny() {
        try {
            ((JavascriptExecutor) driver).executeScript("document.activeElement && document.activeElement.blur();");
        } catch (Exception ignored) {}
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {}
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.cssSelector(".dropdown-menu.show,[role='menu'].show,.time-menu.show,.time-list.show")));
        } catch (TimeoutException ignored) {}
    }

    public void clickNextSmart() {
        debug("Clicking Next button");
        closeOpenMenusIfAny();
        waitForOverlayToDisappear();

        WebElement next = findNextLikeButtonOrNull();
        if (next == null) {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            next = findNextLikeButtonOrNull();
        }
        if (next == null) {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            next = findNextLikeButtonOrNull();
        }
        if (next == null) throw new TimeoutException("Next button not found");

        wait.until(elementIsEnabled(next));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", next);
        safeClick(next);
        waitForOverlayToDisappear();
    }

    private WebElement findNextLikeButtonOrNull() {
        By[] locators = {
                By.xpath("//button[.//span[normalize-space()='Next']]"),
                By.xpath("//button[normalize-space()='Next']"),
                By.cssSelector("button.btn-primary-theme-quote[type='submit']"),
                By.cssSelector("button[type='submit']:not([disabled])")
        };
        for (By by : locators) {
            for (WebElement el : driver.findElements(by)) {
                try {
                    if (el.isDisplayed()) {
                        return el;
                    }
                } catch (StaleElementReferenceException ignored) {}
            }
        }
        return null;
    }

    private List<WebElement> findAddressOptionsForInput(WebElement input) {
        // attempt to get aria-controls from a fresh element (in case input was re-rendered)
        String listId = null;
        try {
            String id = input.getAttribute("id");
            if (id != null && !id.isBlank()) {
                WebElement fresh = driver.findElement(By.id(id));
                listId = fresh.getAttribute("aria-controls");
            } else {
                listId = input.getAttribute("aria-controls");
            }
        } catch (Exception ignored) {}

        if (listId != null && !listId.isBlank()) {
            List<WebElement> inside = driver.findElements(By.cssSelector(
                    "#" + listId + " [role='option'], #" + listId + " li, #" + listId + " .list-group-item, #" + listId + " .autocomplete-item"));
            if (!inside.isEmpty())
                return inside;
        }
        try {
            WebElement container = input.findElement(By.xpath(
                    "(.//following::ul[contains(@class,'autocomplete')][1] | " +
                            ".//following::div[contains(@class,'autocomplete')][1] | " +
                            ".//following::div[contains(@class,'dropdown-menu')][1] | " +
                            ".//following::ul[contains(@class,'dropdown-menu')][1])"));
            List<WebElement> rows = container.findElements(By.cssSelector(
                    "[role='option'], li, .list-group-item, .autocomplete-item, .dropdown-item"));
            if (!rows.isEmpty())
                return rows;
        } catch (NoSuchElementException ignored) {
        } catch (StaleElementReferenceException ignored) {
        }
        List<WebElement> pac = driver.findElements(By.cssSelector(".pac-container .pac-item"));
        if (!pac.isEmpty())
            return pac;
        return driver.findElements(By.cssSelector(".dropdown-menu.show .dropdown-item, .dropdown-menu.show li, [role='listbox'] [role='option']"));
    }

    private ExpectedCondition<Boolean> attributeToBeNotEmpty(final By locator, final String attribute) {
        return driver -> {
            try {
                WebElement element = driver.findElement(locator);
                String attrValue = element.getAttribute(attribute);
                return attrValue != null && !attrValue.trim().isEmpty();
            } catch (NoSuchElementException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        };
    }

    private void waitForFlagReady() {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
            ExpectedConditions.and(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".iti__selected-flag")),
                attributeToBeNotEmpty(By.cssSelector(".iti__selected-flag"), "title")
            )
        );
    }

    private void clearAndType(WebElement element, String text) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a")); // Select all existing text
        element.sendKeys(Keys.BACK_SPACE);               // Delete selected text
        element.clear();                                 // Clear any remaining text
        element.sendKeys(text);                          // Type new text
    }

    private boolean tickTermsFromProvidedMarkup() {
        String script =
            "const wrap = document.querySelector('.checkbox-agree');" +
            "if(!wrap) return false;" +
            "wrap.querySelectorAll('a[href*=\"/privacy\"],a[href*=\"/terms\"]').forEach(a=>{a.__pe=a.style.pointerEvents; a.style.pointerEvents='none'; setTimeout(() => a.style.pointerEvents=a.__pe||'',1500);});" +
            "const cb = wrap.querySelector(\"input[type='checkbox'][name='terms']\");" +
            "if(!cb) return false;" +
            "try { cb.scrollIntoView({block:'center'}); } catch(e) {}" +
            "cb.checked = true;" +
            "['input','change','click','blur'].forEach(ev => cb.dispatchEvent(new Event(ev, {bubbles:true}))); " +
            "return cb.checked === true;";
        Object ok = ((JavascriptExecutor) driver).executeScript(script);
        return Boolean.TRUE.equals(ok);
    }

    private boolean jsTickTermsInCurrentContext() {
        String script =
            "const mark=(el) => { if(!el) return false; el.checked=true; ['input','change','click','blur'].forEach(e => el.dispatchEvent(new Event(e, {bubbles:true}))); return true;};" +
            "let cb = document.querySelector(\"input[type='checkbox'][name='terms']\");" +
            "if(!cb) cb = document.querySelector(\"label.custom-checkbox input[type='checkbox']\");" +
            "if(!cb) cb = Array.from(document.querySelectorAll(\"input[type='checkbox']\")).find(c => (c.name || '').toLowerCase().includes('term') || (c.id || '').toLowerCase().includes('term'));" +
            "if(!cb) cb = Array.from(document.querySelectorAll('label,.custom-checkbox,.form-check,.checkbox-agree'))" +
            "  .filter(n => (n.textContent || '').toLowerCase().includes('term'))" +
            "  .map(n => n.querySelector(\"input[type='checkbox']\")).find(Boolean);" +
            "if(cb) { try { cb.scrollIntoView({block:'center'});} catch(e) {};" +
            "const lab = cb.closest('label'); if(lab) { lab.querySelectorAll('a').forEach(a => { a.__pe = a.style.pointerEvents; a.style.pointerEvents = 'none'; setTimeout(() => a.style.pointerEvents = a.__pe || '', 1500); }); }" +
            "return mark(cb);} return false;";
        Object ok = ((JavascriptExecutor) driver).executeScript(script);
        return Boolean.TRUE.equals(ok);
    }

    private boolean tickTermsSmartWithIframeSearch() {
        if (tickTermsFromProvidedMarkup()) return true;
        if (jsTickTermsInCurrentContext()) return true;

        List<WebElement> frames = driver.findElements(By.tagName("iframe"));
        for (int i = 0; i < frames.size(); i++) {
            try {
                driver.switchTo().frame(i);
                if (tickTermsFromProvidedMarkup() || jsTickTermsInCurrentContext()) {
                    driver.switchTo().defaultContent();
                    return true;
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    driver.switchTo().defaultContent();
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    public String selectAddressByIndex(String inputId, String query, int n) {
        if (n < 1) n = 1;

        // find input fresh each time
        WebElement input = findVisible(By.id(inputId));
        safeClick(input);
        input.clear();
        input.sendKeys(query);

        WebDriverWait suggestionWait = new WebDriverWait(driver, Duration.ofSeconds(12));
        List<WebElement> options = suggestionWait.until(drv -> {
            try {
                // re-find input to avoid stale
                WebElement freshInput = drv.findElement(By.id(inputId));
                List<WebElement> opts = findAddressOptionsForInput(freshInput);
                return (!opts.isEmpty()) ? opts : null;
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return null;
            }
        });

        if (options == null || options.isEmpty()) throw new TimeoutException("No address suggestions found");

        int idx = Math.min(n - 1, options.size() - 1);
        WebElement option = options.get(idx);

        suggestionWait.until(ExpectedConditions.elementToBeClickable(option));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", option);
        safeClick(option);

        // Wait until the input value changes (re-find input inside lambda)
        suggestionWait.until(drv -> {
            try {
                WebElement fresh = drv.findElement(By.id(inputId));
                String v = fresh.getAttribute("value");
                return v != null && v.trim().length() > 3 && !v.trim().equalsIgnoreCase(query.trim());
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });

        // Wait until validation class (if any) clears - re-find inside lambda
        suggestionWait.until(drv -> {
            try {
                WebElement fresh = drv.findElement(By.id(inputId));
                String cls = fresh.getAttribute("class");
                return cls == null || !cls.contains("is-invalid");
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });

        String selected = retryStale(() -> driver.findElement(By.id(inputId)).getAttribute("value"));
        debug("Selected address for " + inputId + ": " + selected);
        return selected;
    }

    public String selectPickupByIndex(String q, int n) {
        return selectAddressByIndex("fetch_postcode", q, n);
    }

    public String selectDropByIndex(String q, int n) {
        return selectAddressByIndex("fetch_address", q, n);
    }

    public void waitForStep3() {
        debug("Waiting for Step 3: first_name input visible");
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[name='first_name']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    public void clickSubmit(String mainHandle) {
        debug("Step3: click Submit");
        By[] candidates = new By[]{
                By.cssSelector("button.next[type='submit']"),
                By.xpath("//button[@type='submit' and .//span[normalize-space()='Submit']]"),
                By.xpath("//button[@type='submit' and (contains(.,'Submit') or contains(.,'SUBMIT'))]"),
                By.cssSelector("button.btn-primary-theme-quote.next[type='submit']")
        };
        WebElement btn = null;
        for (By by : candidates) {
            List<WebElement> found = driver.findElements(by);
            if (!found.isEmpty()) {
                try {
                    if (found.get(0).isDisplayed()) {
                        btn = found.get(0);
                        break;
                    }
                } catch (StaleElementReferenceException ignored) {}
            }
        }
        if (btn == null) throw new NoSuchElementException("Submit button not found");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        wait.until(ExpectedConditions.elementToBeClickable(btn));
        safeClick(btn);
        waitForOverlayToDisappear();
        closeUnexpectedTabsExcept(mainHandle);
    }

    public void fillCustomerDetails(String first, String last, String email, String phone, boolean agreeTerms, String mainHandle) {
        debug("Step3: fill details");

        WebElement firstName = findVisible(By.xpath("//input[@placeholder='Enter First Name']"));
        WebElement lastName = findVisible(By.cssSelector("input[name='sur_name']"));
        WebElement emailEl = findVisible(By.cssSelector("input[name='email']"));

        waitForFlagReady();

        By phoneLocator = By.cssSelector("input[name='phone'], input[id*='phone']");
        WebElement phoneEl = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(phoneLocator));

        clearAndType(firstName, first);
        clearAndType(lastName, last);
        clearAndType(emailEl, email);
        clearAndType(phoneEl, phone);

        ((JavascriptExecutor) driver).executeScript(
                "['input','change','blur'].forEach(e=>arguments[0].dispatchEvent(new Event(e,{bubbles:true})));",
                phoneEl);

        if (agreeTerms) {
            boolean ticked = tickTermsSmartWithIframeSearch();
            if (!ticked) throw new TimeoutException("Could not find the Terms checkbox inside .checkbox-agree.");
            closeUnexpectedTabsExcept(mainHandle);
        }
    }

    public void runFlowWithAddressIndices(String term,
                                          String dateDdMmYyyy,
                                          String timeText,
                                          String pickupQuery, int pickupIdx,
                                          String dropQuery, int dropIdx) {

        chooseCategory(term);
        chooseFirstSubcategory();
        waitForSubcategoryChecked();

        clickNextSmart();
        waitForDateStep();

        setDateByJS(dateDdMmYyyy);

        try {
            pickTimeByText(timeText);
        } catch (TimeoutException | NoSuchElementException e) {
            setTimeByJS(timeText);
        }

        selectPickupByIndex(pickupQuery, pickupIdx);
        selectDropByIndex(dropQuery, dropIdx);

        clickNextSmart();
    }

}
