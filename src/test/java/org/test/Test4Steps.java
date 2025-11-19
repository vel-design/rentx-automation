package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class Test4Steps {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public Test4Steps(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    private void debug(String msg) { System.out.println("[DEBUG] " + msg); }

    private void safeClick(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (ElementClickInterceptedException | TimeoutException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private WebElement findVisible(By locator) { return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)); }

    private void nap(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void waitForOverlayToDisappear() {
        By overlay = By.cssSelector(".loading-overlay, .spinner, .blockUI, .modal-backdrop.show, .overlay, .page-loader");
        try { new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.invisibilityOfElementLocated(overlay));
        } catch (TimeoutException ignored) {}
    }

    private void closeUnexpectedTabsExcept(String mainHandle) {
        try {
            Set<String> all = driver.getWindowHandles();
            for (String h : all) {
                if (!h.equals(mainHandle)) {
                    driver.switchTo().window(h);
                    driver.close();
                }
            }
            driver.switchTo().window(mainHandle);
        } catch (Exception ignored) {}
    }

    public void chooseCategory(String searchTerm) {
        debug("Category: focusing input");
        WebElement categoryInput = findVisible(By.id("fetch_service"));
        safeClick(categoryInput);
        categoryInput.clear();
        categoryInput.sendKeys(searchTerm);

        String listId = categoryInput.getAttribute("aria-controls");
        if (listId == null || listId.isBlank()) listId = "autoComplete_list_1";

        debug("Category: waiting for suggestions #" + listId);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(listId)));

        By options = By.cssSelector("#" + listId + " [role='option']");
        List<WebElement> items = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(options));
        if (items.isEmpty()) throw new TimeoutException("No autocomplete options found in #" + listId);

        debug("Category: clicking first option");
        safeClick(items.get(0));
        nap(150);
    }

    public void chooseFirstSubcategory() {
        By subcatLabels = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(subcatLabels));
        if (labels.isEmpty()) throw new RuntimeException("No subcategories found.");
        safeClick(labels.get(0));
        nap(150);
    }

    private void waitForSubcategoryChecked() {
        debug("Subcat: waiting for checked radio");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[name='type_of_service']:checked")));
    }

    private void waitForDateStep() {
        debug("Step 2: waiting for date input");
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name='start_date']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        nap(150);
    }

    public void setDateByJS(String ddMMyyyyWithDashes) {
        debug("Date(JS): " + ddMMyyyyWithDashes);
        WebElement dateInput = findVisible(By.cssSelector("input[name='start_date']"));
        String js = "const el=arguments[0],v=arguments[1];el.value=v;el.setAttribute('value',v);" +
                "['input','change','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true})));";
        ((JavascriptExecutor) driver).executeScript(js, dateInput, ddMMyyyyWithDashes);
        String got = dateInput.getAttribute("value");
        if (got == null || !got.trim().equals(ddMMyyyyWithDashes)) {
            throw new RuntimeException("Date not set: " + got);
        }
    }

    public void pickTimeByText(String timeTextDesired) {
        debug("Time: open dropdown");
        WebElement trigger = findVisible(By.id("customDropdownButton"));
        safeClick(trigger);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".dropdown-menu.show, [role='menu'].show, .time-menu.show, .time-list.show")));

        List<WebElement> selects = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//button[normalize-space()='Select']")));
        if (selects.isEmpty()) throw new TimeoutException("No time rows");

        String desired = normalizeTime(timeTextDesired);
        String desiredNo0 = desired.replaceAll("\\b0([1-9]:)", "$1");

        for (WebElement btn : selects) {
            WebElement row = btn;
            try { row = btn.findElement(By.xpath("./ancestor::*[self::tr or self::li or self::div][1]")); }
            catch (Exception ignored) {}
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", row);
            String txt = normalizeTime(row.getText());
            if (txt.contains(desired) || txt.contains(desiredNo0)) { safeClick(btn); return; }
        }
        safeClick(selects.get(0)); // fallback
    }

    public void setTimeByJS(String timeText) {
        debug("Time(JS): " + timeText);
        WebElement timeInput = findVisible(By.cssSelector("input[name='start_time']"));
        String js = "const el=arguments[0],v=arguments[1];el.value=v;el.setAttribute('value',v);" +
                "['input','change','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true})));";
        ((JavascriptExecutor) driver).executeScript(js, timeInput, timeText);
    }

    private String normalizeTime(String s) {
        if (s == null) return "";
        String t = s.replace('\u00A0',' ').replaceAll("[\\s\\n\\r]+"," ").trim();
        t = t.replaceAll("(?i)am","AM").replaceAll("(?i)pm","PM");
        return t;
    }

    private void closeOpenMenusIfAny() {
        try { ((JavascriptExecutor) driver).executeScript("document.activeElement && document.activeElement.blur();"); } catch (Exception ignored) {}
        try { driver.switchTo().activeElement().sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".dropdown-menu.show,[role='menu'].show,.time-menu.show,.time-list.show")));
        } catch (TimeoutException ignored) {}
    }

    private ExpectedCondition<Boolean> elementIsEnabled(WebElement el) {
        return d -> el != null && el.isDisplayed() && el.isEnabled()
                && !"true".equalsIgnoreCase(el.getAttribute("aria-disabled"))
                && (el.getAttribute("disabled") == null)
                && !(String.valueOf(el.getAttribute("class")).toLowerCase().contains("disabled"));
    }

    private WebElement findNextLikeButtonOrNull() {
        By[] locs = {
                By.xpath("//button[.//span[normalize-space()='Next']]"),
                By.xpath("//button[normalize-space()='Next']"),
                By.cssSelector("button.btn-primary-theme-quote[type='submit']"),
                By.cssSelector("button[type='submit']:not([disabled])")
        };
        for (By by : locs) {
            for (WebElement el : driver.findElements(by)) if (el.isDisplayed()) return el;
        }
        return null;
    }

    public void clickNextSmart() {
        closeOpenMenusIfAny(); waitForOverlayToDisappear();
        WebElement next = findNextLikeButtonOrNull();
        if (next == null) { ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);"); nap(120); next = findNextLikeButtonOrNull(); }
        if (next == null) { ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);"); nap(120); next = findNextLikeButtonOrNull(); }
        if (next == null) throw new TimeoutException("Next not found");
        new WebDriverWait(driver, Duration.ofSeconds(8)).until(elementIsEnabled(next));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", next);
        safeClick(next); waitForOverlayToDisappear();
    }

    private List<WebElement> findAddressOptionsForInput(WebElement input) {
        String listId = input.getAttribute("aria-controls");
        if (listId != null && !listId.isBlank()) {
            List<WebElement> inside = driver.findElements(By.cssSelector(
                    "#" + listId + " [role='option'], #" + listId + " li, #" + listId + " .list-group-item, #" + listId + " .autocomplete-item"));
            if (!inside.isEmpty()) return inside;
        }
        try {
            WebElement cont = input.findElement(By.xpath(
                    "(.//following::ul[contains(@class,'autocomplete')][1] | " +
                            ".//following::div[contains(@class,'autocomplete')][1] | " +
                            ".//following::div[contains(@class,'dropdown-menu')][1] | " +
                            ".//following::ul[contains(@class,'dropdown-menu')][1])"));
            List<WebElement> rows = cont.findElements(By.cssSelector(
                    "[role='option'], li, .list-group-item, .autocomplete-item, .dropdown-item"));
            if (!rows.isEmpty()) return rows;
        } catch (NoSuchElementException ignored) {}
        List<WebElement> pac = driver.findElements(By.cssSelector(".pac-container .pac-item"));
        if (!pac.isEmpty()) return pac;
        return driver.findElements(By.cssSelector(".dropdown-menu.show .dropdown-item, .dropdown-menu.show li, [role='listbox'] [role='option']"));
    }

    public String selectAddressByIndex(String inputId, String query, int n) {
        if (n < 1) n = 1;
        WebElement input = findVisible(By.id(inputId));
        safeClick(input); input.clear(); input.sendKeys(query);

        List<WebElement> options = new ArrayList<>();
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            options = findAddressOptionsForInput(input);
            if (!options.isEmpty()) break;
            nap(120);
        }
        if (options.isEmpty()) throw new TimeoutException("No address suggestions for: " + query);

        int idx = Math.min(n - 1, options.size() - 1);
        WebElement option = options.get(idx);

        WebElement clickable = null;
        try { clickable = option.findElement(By.cssSelector("a, button")); } catch (NoSuchElementException ignored) {}
        WebElement target = (clickable != null ? clickable : option);

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", target);
        safeClick(target);

        wait.until(d -> {
            String v = input.getAttribute("value");
            return v != null && v.trim().length() > 3;
        });
        return input.getAttribute("value");
    }

    public String selectPickupByIndex(String q, int n) { return selectAddressByIndex("fetch_postcode", q, n); }
    public String selectDropByIndex(String q, int n)   { return selectAddressByIndex("fetch_address",  q, n); }

    public void waitForStep3() {
        debug("Step3: wait first_name");
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[name='first_name']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    private void clearAndType(WebElement el, String text) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        el.clear();
        el.sendKeys(text);
    }

    private boolean tickTermsFromProvidedMarkup() {
        String script =
            "const wrap = document.querySelector('.checkbox-agree');" +
            "if(!wrap) return false;" +
            "wrap.querySelectorAll('a[href*=\"/privacy\"],a[href*=\"/terms\"]').forEach(a=>{a.__pe=a.style.pointerEvents; a.style.pointerEvents='none'; setTimeout(()=>a.style.pointerEvents=a.__pe||'',1500);});" +
            "const cb = wrap.querySelector(\"input[type='checkbox'][name='terms']\");" +
            "if(!cb) return false;" +
            "try{ cb.scrollIntoView({block:'center'}); }catch(e){}" +
            "cb.checked = true;" +
            "['input','change','click','blur'].forEach(ev=>cb.dispatchEvent(new Event(ev,{bubbles:true}))); " +
            "return cb.checked === true;";
        Object ok = ((JavascriptExecutor) driver).executeScript(script);
        return Boolean.TRUE.equals(ok);
    }

    private boolean jsTickTermsInCurrentContext() {
        String script =
            "const mark=(el)=>{if(!el)return false; el.checked=true; ['input','change','click','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true}))); return true;};" +
            "let cb=document.querySelector(\"input[type='checkbox'][name='terms']\");" +
            "if(!cb) cb=document.querySelector(\"label.custom-checkbox input[type='checkbox']\");" +
            "if(!cb) cb=Array.from(document.querySelectorAll(\"input[type='checkbox']\")).find(c=>(c.name||'').toLowerCase().includes('term')||(c.id||'').toLowerCase().includes('term'));" +
            "if(!cb) cb=Array.from(document.querySelectorAll('label,.custom-checkbox,.form-check,.checkbox-agree'))" +
            "  .filter(n=>(n.textContent||'').toLowerCase().includes('term'))" +
            "  .map(n=>n.querySelector(\"input[type='checkbox']\")).find(Boolean);" +
            "if(cb){ try{cb.scrollIntoView({block:'center'});}catch(e){};" +
            " const lab=cb.closest('label'); if(lab){lab.querySelectorAll('a').forEach(a=>{a.__pe=a.style.pointerEvents; a.style.pointerEvents='none'; setTimeout(()=>a.style.pointerEvents=a.__pe||'',1500);});}" +
            " return mark(cb);} return false;";
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
                try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            }
        }
        return false;
    }
    private ExpectedCondition<Boolean> attributeToBeNotEmpty(final By locator, final String attribute) {
        return driver -> {
            try {
                WebElement element = driver.findElement(locator);
                String attrValue = element.getAttribute(attribute);
                return attrValue != null && !attrValue.trim().isEmpty();
            } catch (NoSuchElementException e) {
                return false;
            }
        };
    }


    private void waitForFlagReady() {
    	new WebDriverWait(driver, Duration.ofSeconds(10)).until(
    		    ExpectedConditions.and(
    		        ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".iti__selected-flag")),
    		        attributeToBeNotEmpty(By.cssSelector(".iti__selected-flag"), "title")
    		    
        ));
    }

    public void fillCustomerDetails(String first, String last, String email, String phone, boolean agreeTerms, String mainHandle) {
        debug("Step3: fill details");

        WebElement firstName = findVisible(By.xpath("//input[@placeholder='Enter First Name']"));
        WebElement lastName  = findVisible(By.cssSelector("input[name='sur_name']"));
        WebElement emailEl   = findVisible(By.cssSelector("input[name='email']"));

        // Wait for the phone input flag UI to be ready
        waitForFlagReady();

        By phoneLocator = By.cssSelector("input[name='phone'], input[id*='phone']");
        WebElement phoneEl = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(phoneLocator));

        clearAndType(firstName, first);
        clearAndType(lastName,  last);
        clearAndType(emailEl,   email);
        clearAndType(phoneEl,   phone);

        ((JavascriptExecutor) driver).executeScript(
                "['input','change','blur'].forEach(e=>arguments[0].dispatchEvent(new Event(e,{bubbles:true})));",
                phoneEl);

        if (agreeTerms) {
            boolean ticked = tickTermsSmartWithIframeSearch();
            if (!ticked) throw new TimeoutException("Could not find the Terms checkbox inside .checkbox-agree.");
            closeUnexpectedTabsExcept(mainHandle);
        }
    }

    public void clickSubmit(String mainHandle) {
        debug("Step3: click Submit");
        By[] candidates = new By[] {
                By.cssSelector("button.next[type='submit']"),
                By.xpath("//button[@type='submit' and .//span[normalize-space()='Submit']]"),
                By.xpath("//button[@type='submit' and (contains(.,'Submit') or contains(.,'SUBMIT'))]"),
                By.cssSelector("button.btn-primary-theme-quote.next[type='submit']")
        };
        WebElement btn = null;
        for (By by : candidates) {
            List<WebElement> found = driver.findElements(by);
            if (!found.isEmpty() && found.get(0).isDisplayed()) { btn = found.get(0); break; }
        }
        if (btn == null) throw new NoSuchElementException("Submit button not found");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        wait.until(ExpectedConditions.elementToBeClickable(btn));
        safeClick(btn);
        waitForOverlayToDisappear();
        closeUnexpectedTabsExcept(mainHandle);
    }

    public void runFlowWithAddressIndices(String term,
                                          String dateDdMmYyyy,
                                          String timeText,
                                          String pickupQuery, int pickupIdx,
                                          String dropQuery,   int dropIdx) {

        chooseCategory(term);
        chooseFirstSubcategory();
        waitForSubcategoryChecked();

        clickNextSmart();
        waitForDateStep();

        setDateByJS(dateDdMmYyyy);

        try { pickTimeByText(timeText); }
        catch (TimeoutException | NoSuchElementException e) { setTimeByJS(timeText); }

        selectPickupByIndex(pickupQuery, pickupIdx);
        selectDropByIndex(dropQuery, dropIdx);

        clickNextSmart();
    }

}
