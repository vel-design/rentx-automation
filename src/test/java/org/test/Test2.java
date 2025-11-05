package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

public class Test2 {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public Test2(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /* ========================= Helpers ========================= */

    private void debug(String msg) { System.out.println("[DEBUG] " + msg); }

    /** Scroll into view and click; fall back to JS click if intercepted. */
    private void safeClick(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (ElementClickInterceptedException | TimeoutException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    /** Wait until visible, then return element. */
    private WebElement findVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private static void nap(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    /** If your app shows overlays/spinners, wait for them to disappear. Update selectors if needed. */
    private void waitForOverlayToDisappear() {
        By overlay = By.cssSelector(".loading-overlay, .spinner, .blockUI, .modal-backdrop.show, .overlay, .page-loader");
        try { new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.invisibilityOfElementLocated(overlay));
        } catch (TimeoutException ignored) {}
    }

    /* ====================== Step 1: Category ====================== */

    /** Type term and select the FIRST autocomplete suggestion. */
    public void chooseCategory(String searchTerm) {
        debug("Category: focusing input");
        WebElement categoryInput = findVisible(By.id("fetch_service"));
        safeClick(categoryInput);
        categoryInput.clear();
        categoryInput.sendKeys(searchTerm);

        String listId = categoryInput.getAttribute("aria-controls");
        if (listId == null || listId.isBlank()) listId = "autoComplete_list_1";

        debug("Category: waiting for suggestions #" + listId);
        By suggestionList = By.id(listId);
        wait.until(ExpectedConditions.visibilityOfElementLocated(suggestionList));

        By options = By.cssSelector("#" + listId + " [role='option']");
        List<WebElement> items = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(options));
        if (items.isEmpty()) throw new TimeoutException("No autocomplete options found in #" + listId);

        debug("Category: clicking first option");
        safeClick(items.get(0));
        nap(150);
    }

    /* ==================== Step 1b: Subcategory ==================== */

    /** Pick FIRST subcategory (radio + label). */
    public void chooseFirstSubcategory() {
        By subcatLabels = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(subcatLabels));
        if (labels.isEmpty()) throw new RuntimeException("No subcategories found.");
        safeClick(labels.get(0));
        nap(150);
    }

    /** Ensure a subcategory radio is actually checked. */
    private void waitForSubcategoryChecked() {
        debug("Subcat: waiting for checked radio");
        By checkedRadio = By.cssSelector("input[name='type_of_service']:checked");
        wait.until(ExpectedConditions.presenceOfElementLocated(checkedRadio));
    }

    /* ================= Step 2: Date & Time (JS + robust UI) ================= */

    /** Wait until Step 2 shows and the date input is visible & in view. */
    private void waitForDateStep() {
        debug("Step 2: waiting for date input to be visible & interactable");
        By dateInput = By.cssSelector("input[name='start_date']");
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(dateInput));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        nap(150);
    }

    /** Set date directly and fire events. Format: dd-MM-yyyy (e.g., "19-09-2025"). */
    public void setDateByJS(String ddMMyyyyWithDashes) {
        debug("Date(JS): setting -> " + ddMMyyyyWithDashes);
        By dateLocator = By.cssSelector("input[name='start_date']");
        WebElement dateInput = findVisible(dateLocator);

        String js =
            "const el = arguments[0];" +
            "const val = arguments[1];" +
            "el.value = val;" +
            "el.setAttribute('value', val);" +
            "el.dispatchEvent(new Event('input', {bubbles:true}));" +
            "el.dispatchEvent(new Event('change', {bubbles:true}));" +
            "el.dispatchEvent(new Event('blur', {bubbles:true}));";
        ((JavascriptExecutor) driver).executeScript(js, dateInput, ddMMyyyyWithDashes);

        String got = dateInput.getAttribute("value");
        if (got == null || !got.trim().equals(ddMMyyyyWithDashes)) {
            throw new RuntimeException("Date field did not accept value: expected '" +
                    ddMMyyyyWithDashes + "' but saw '" + got + "'");
        }
        nap(150);
    }

    /** Open time menu and click the Select of the row that matches the desired time; fallback to first. */
    public void pickTimeByText(String timeTextDesired) {
        debug("Time: opening dropdown trigger");
        WebElement trigger = findVisible(By.id("customDropdownButton"));
        safeClick(trigger);

        debug("Time: waiting for dropdown/menu container to show");
        By anyMenu = By.cssSelector(".dropdown-menu.show, [role='menu'].show, .time-menu.show, .time-list.show");
        wait.until(ExpectedConditions.visibilityOfElementLocated(anyMenu));

        List<WebElement> selectButtons = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//button[normalize-space()='Select']"))
        );
        if (selectButtons.isEmpty()) throw new TimeoutException("No 'Select' buttons found in time list.");

        String desired = normalizeTime(timeTextDesired);
        String desiredNoLeadZero = desired.replaceAll("\\b0([1-9]:)", "$1");

        for (WebElement selectBtn : selectButtons) {
            WebElement row = selectBtn;
            try { row = selectBtn.findElement(By.xpath("./ancestor::*[self::tr or self::li or self::div][1]")); }
            catch (Exception ignored) {}

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", row);
            nap(80);

            String rowText = normalizeTime(row.getText());
            if (rowText.contains(desired) || rowText.contains(desiredNoLeadZero)) {
                debug("Time: match found -> " + rowText.replace("\n"," | "));
                safeClick(selectBtn);
                nap(120);
                return;
            }
        }

        debug("Time: exact match not found; clicking first available time.");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", selectButtons.get(0));
        safeClick(selectButtons.get(0));
        nap(120);
    }

    /** JS fallback for time, if your app accepts programmatic value. */
    public void setTimeByJS(String timeText) {
        debug("Time(JS): setting -> " + timeText);
        WebElement timeInput = findVisible(By.cssSelector("input[name='start_time']"));
        String js =
            "const el = arguments[0];" +
            "const val = arguments[1];" +
            "el.value = val;" +
            "el.setAttribute('value', val);" +
            "el.dispatchEvent(new Event('input', {bubbles:true}));" +
            "el.dispatchEvent(new Event('change', {bubbles:true}));" +
            "el.dispatchEvent(new Event('blur', {bubbles:true}));";
        ((JavascriptExecutor) driver).executeScript(js, timeInput, timeText);
    }

    private String normalizeTime(String s) {
        if (s == null) return "";
        String t = s.replace('\u00A0',' ').replaceAll("[\\s\\n\\r]+"," ").trim();
        t = t.replaceAll("(?i)am", "AM").replaceAll("(?i)pm", "PM");
        return t;
    }

    /* ====================== Next button (SMART) ====================== */

    /** Close any open Bootstrap/menus so they don't cover the Next button. */
    private void closeOpenMenusIfAny() {
        try { ((JavascriptExecutor) driver).executeScript("document.activeElement && document.activeElement.blur();"); } catch (Exception ignored) {}
        try { driver.switchTo().activeElement().sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}

        try {
            WebElement body = driver.findElement(By.tagName("body"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", body);
        } catch (Exception ignored) {}

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".dropdown-menu.show, [role='menu'].show, .time-menu.show, .time-list.show")
            ));
        } catch (TimeoutException ignored) {}
    }

    private ExpectedCondition<Boolean> elementIsEnabled(WebElement el) {
        return drv -> {
            if (el == null) return false;
            String disabled = el.getAttribute("disabled");
            String aria = el.getAttribute("aria-disabled");
            String cls = el.getAttribute("class");
            boolean hasDisabledClass = cls != null && cls.toLowerCase().contains("disabled");
            return el.isDisplayed() && el.isEnabled() &&
                   (disabled == null) && !"true".equalsIgnoreCase(aria) && !hasDisabledClass;
        };
    }

    /** Try several robust locators for the Next button; return null if none found quickly. */
    private WebElement findNextLikeButtonOrNull() {
        By[] locators = new By[] {
            By.xpath("//button[.//span[normalize-space()='Next']]"),
            By.xpath("//button[normalize-space()='Next']"),
            By.xpath("//button[normalize-space()='NEXT']"),
            By.xpath("//button[contains(.,'Next') or contains(.,'NEXT') or contains(.,'Continue') or contains(.,'Proceed') or contains(.,'Get Quotes')]"),
            By.cssSelector("button.btn-primary-theme-quote[type='submit']"),
            By.cssSelector("button[type='submit']:not([disabled])")
        };

        for (By by : locators) {
            List<WebElement> found = driver.findElements(by);
            for (WebElement el : found) {
                if (el.isDisplayed()) return el;
            }
        }
        return null;
    }

    /** Submit the first visible form on the page via JS (requestSubmit triggers native submit). */
    private boolean trySubmitVisibleForm() {
        try {
            WebElement form = driver.findElement(By.cssSelector("form:has(button), form"));
            ((JavascriptExecutor) driver).executeScript(
                "if (arguments[0].requestSubmit) arguments[0].requestSubmit(); else arguments[0].submit();",
                form
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Smart Next: close menus, find Next/Continue, ensure enabled, scroll, click; fallback to form submit. */
    public void clickNextSmart() {
        debug("NextSmart: closing open menus if any");
        closeOpenMenusIfAny();
        waitForOverlayToDisappear();

        debug("NextSmart: locating Next-like button");
        WebElement next = findNextLikeButtonOrNull();

        if (next == null) {
            debug("NextSmart: not found; scroll to bottom and retry");
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            nap(150);
            closeOpenMenusIfAny();
            next = findNextLikeButtonOrNull();
        }

        if (next == null) {
            debug("NextSmart: still not found; scroll to top and retry");
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            nap(150);
            next = findNextLikeButtonOrNull();
        }

        if (next == null) {
            debug("NextSmart: Next-like button not found; attempting form submit fallback");
            boolean submitted = trySubmitVisibleForm();
            if (!submitted) throw new TimeoutException("Next button not found and form submit fallback failed.");
            waitForOverlayToDisappear();
            return;
        }

        debug("NextSmart: waiting until button is enabled");
        new WebDriverWait(driver, Duration.ofSeconds(8)).until(elementIsEnabled(next));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", next);
        nap(100);
        debug("NextSmart: clicking");
        safeClick(next);

        waitForOverlayToDisappear();
    }

    /* =================== Address suggestions (rows) =================== */

    /** Find visible suggestion OPTION rows for an address input (not "Select" buttons). */
    private List<WebElement> findAddressOptionsForInput(WebElement input) {
        String listId = input.getAttribute("aria-controls");
        if (listId != null && !listId.isBlank()) {
            By optInList = By.cssSelector(
                "#" + listId + " [role='option'], " +
                "#" + listId + " li, " +
                "#" + listId + " .list-group-item, " +
                "#" + listId + " .autocomplete-item"
            );
            List<WebElement> inside = driver.findElements(optInList);
            if (!inside.isEmpty()) return inside;
        }

        try {
            WebElement container = input.findElement(By.xpath(
                "(.//following::ul[contains(@class,'autocomplete')][1] | " +
                " .//following::div[contains(@class,'autocomplete')][1] | " +
                " .//following::div[contains(@class,'dropdown-menu')][1] | " +
                " .//following::ul[contains(@class,'dropdown-menu')][1])"
            ));
            List<WebElement> rows = container.findElements(By.cssSelector(
                "[role='option'], li, .list-group-item, .autocomplete-item, .dropdown-item"
            ));
            if (!rows.isEmpty()) return rows;
        } catch (NoSuchElementException ignored) {}

        List<WebElement> pac = driver.findElements(By.cssSelector(".pac-container .pac-item"));
        if (!pac.isEmpty()) return pac;

        List<WebElement> generic = driver.findElements(By.cssSelector(
            ".dropdown-menu.show .dropdown-item, .dropdown-menu.show li, [role='listbox'] [role='option']"
        ));
        return generic;
    }

    /**
     * Type query into an address input and click the n-th suggestion ROW.
     * n is 1-based: 1 = first, 2 = second, ... (clamped to last if too large).
     * Returns the final value filled into the input.
     */
    public String selectAddressByIndex(String inputId, String query, int n) {
        if (n < 1) n = 1;

        WebElement input = findVisible(By.id(inputId));
        safeClick(input);
        input.clear();
        input.sendKeys(query);

        List<WebElement> options = new java.util.ArrayList<>();
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            options = findAddressOptionsForInput(input);
            if (!options.isEmpty()) break;
            nap(150);
        }
        if (options.isEmpty()) {
            throw new TimeoutException("No address suggestions appeared for query: " + query);
        }

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
        String finalVal = input.getAttribute("value");
        debug("AddrByIndex: picked " + n + " / " + options.size() + " -> " + finalVal);
        return finalVal;
    }

    // Convenience wrappers (1-based index) for pickup / drop
    public String selectPickupByIndex(String query, int n) { return selectAddressByIndex("fetch_postcode", query, n); }
    public String selectDropByIndex(String query, int n)   { return selectAddressByIndex("fetch_address",  query, n); }

    /* ========================== Step 3 (ADDED) ========================== */

    /** Wait until Step 3 (details form) is ready by watching the First Name field. */
    public void waitForStep3() {
        debug("Step3: waiting for First Name input");
        By firstName = By.cssSelector("input[name='first_name']");
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(firstName));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    /** Clear input robustly (intl-tel / masked friendly) and type text. */
    private void clearAndType(WebElement el, String text) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        el.clear();
        el.sendKeys(text);
    }

    /** Fill First/Last/Email/Phone and tick Terms. */
    public void fillCustomerDetails(String first, String last, String email, String phone, boolean agreeTerms) {
        debug("Step3: filling customer details");

        WebElement firstName = findVisible(By.cssSelector("input[name='first_name']"));
        WebElement lastName  = findVisible(By.cssSelector("input[name='sur_name']"));
        WebElement emailEl   = findVisible(By.cssSelector("input[name='email']"));
        WebElement phoneEl   = findVisible(By.cssSelector("input#phone_[name='phone']"));

        clearAndType(firstName, first);
        clearAndType(lastName,  last);
        clearAndType(emailEl,   email);

        clearAndType(phoneEl, phone);
        ((JavascriptExecutor) driver).executeScript(
            "['input','change','blur'].forEach(e=>arguments[0].dispatchEvent(new Event(e,{bubbles:true})));",
            phoneEl
        );

        WebElement terms = findVisible(By.cssSelector("input[type='checkbox'][name='terms']"));
        if (agreeTerms && !terms.isSelected()) {
            try {
                WebElement label = terms.findElement(By.xpath("./ancestor::label[1]"));
                safeClick(label);
            } catch (NoSuchElementException e) {
                safeClick(terms);
            }
        }

        if (agreeTerms && !terms.isSelected()) throw new RuntimeException("Terms checkbox did not become selected.");
    }

    /** Submit Step 3 (tries explicit Next/Submit then falls back to form.requestSubmit). */
    public void submitStep3() {
        debug("Step3: submit");
        By[] candidates = new By[] {
            By.cssSelector("button[type='submit'].next"),
            By.xpath("//button[@type='submit' and .//span[normalize-space()='Next']]"),
            By.xpath("//button[@type='submit' and (contains(.,'Next') or contains(.,'Submit') or contains(.,'Get Quotes'))]"),
            By.cssSelector("button.btn-primary-theme-quote[type='submit']")
        };

        WebElement btn = null;
        for (By by : candidates) {
            List<WebElement> found = driver.findElements(by);
            if (!found.isEmpty() && found.get(0).isDisplayed()) { btn = found.get(0); break; }
        }

        if (btn != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            safeClick(btn);
            waitForOverlayToDisappear();
            return;
        }

        // Fallback: submit the nearest form
        WebElement form = null;
        try {
            form = driver.findElement(By.xpath("//input[@name='first_name']/ancestor::form[1] | //form"));
        } catch (NoSuchElementException ignored) {}
        if (form == null) throw new NoSuchElementException("Step 3 form not found.");

        ((JavascriptExecutor) driver).executeScript(
            "if(arguments[0].requestSubmit){arguments[0].requestSubmit();}else{arguments[0].submit();}", form);
        waitForOverlayToDisappear();
    }

    /* ========================== Flows ========================== */

    /** Step 1 → Step 2: category, subcategory, date/time, addresses, then continue. */
    public void runFlowWithAddressIndices(String term,
                                          String dateDdMmYyyyWithDashes,
                                          String timeText,
                                          String pickupQuery, int pickupIndex1Based,
                                          String dropQuery,   int dropIndex1Based) {

        debug("Flow: choose category");
        chooseCategory(term);

        debug("Flow: choose first subcategory");
        chooseFirstSubcategory();
        waitForSubcategoryChecked();

        debug("Flow: go to Step 2 (Date & Location)");
        clickNextSmart();
        waitForDateStep();

        debug("Flow: set date via JS");
        setDateByJS(dateDdMmYyyyWithDashes);

        debug("Flow: pick time (robust)");
        try { pickTimeByText(timeText); }
        catch (TimeoutException | NoSuchElementException e) {
            debug("Flow: time UI not matched; using JS fallback");
            setTimeByJS(timeText);
        }

        debug("Flow: pickup by index");
        selectPickupByIndex(pickupQuery, pickupIndex1Based);

        debug("Flow: drop by index");
        selectDropByIndex(dropQuery, dropIndex1Based);

        debug("Flow: proceed to next step");
        clickNextSmart();
    }

    /* ============================ Demo ============================ */

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        try {
            driver.get("https://rentx.com/quote"); // change if using staging

            Test2 selector = new Test2(driver);

            // Step 1 → 2
            selector.runFlowWithAddressIndices(
                "l",
                "19-09-2025",
                "01:00 PM",
                "street", 2,
                "12345", 2
            );

            // Step 3
            selector.waitForStep3();
            selector.fillCustomerDetails(
                "Test",
                "Vel",
                "vel@mobo.co.uk",
                "07875320316",
                true
            );
            selector.submitStep3();

        } finally {
            // driver.quit();
        }
    }
}
