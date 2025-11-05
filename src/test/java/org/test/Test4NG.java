package org.test;

import com.aventstack.extentreports.*;
//import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class Test4NG {

    private WebDriver driver;
    private WebDriverWait wait;
    private ExtentReports extent;
    private ExtentTest test;
    private String mainHandle;

    @BeforeSuite
   /* public void setupReports() {
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("Test4NG_Report.html");
        htmlReporter.config().setDocumentTitle("RentX Quote Automation Report");
        htmlReporter.config().setReportName("RentX Automation TestNG Report");
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        extent.setSystemInfo("Tester", "Vel");
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Browser", "Chrome");
    } */

    @BeforeClass
    public void setupDriver() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        mainHandle = driver.getWindowHandle();
    }

    @AfterMethod
    public void captureFailure(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            try {
                File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                test.fail("Test Failed: " + result.getThrowable());
                test.addScreenCaptureFromPath(src.getAbsolutePath());
            } catch (Exception e) {
                test.warning("Could not capture screenshot: " + e.getMessage());
            }
        }
    }

    @AfterClass
    public void teardownDriver() {
        if (driver != null) driver.quit();
    }

    @AfterSuite
    public void flushReports() {
        if (extent != null) extent.flush();
    }

    private void log(String msg) {
        System.out.println("[DEBUG] " + msg);
        test.info(msg);
    }

    private void safeClick(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private WebElement findVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void nap(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private void waitForOverlayToDisappear() {
        By overlay = By.cssSelector(".loading-overlay, .spinner, .blockUI, .modal-backdrop.show, .overlay, .page-loader");
        try { new WebDriverWait(driver, Duration.ofSeconds(8)).until(ExpectedConditions.invisibilityOfElementLocated(overlay)); }
        catch (TimeoutException ignored) {}
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

    /* ====================== Step 1: Category ====================== */

    private void chooseCategory(String searchTerm) {
        log("Category: focusing input");
        WebElement categoryInput = findVisible(By.id("fetch_service"));
        safeClick(categoryInput);
        categoryInput.clear();
        categoryInput.sendKeys(searchTerm);

        String listId = categoryInput.getAttribute("aria-controls");
        if (listId == null || listId.isBlank()) listId = "autoComplete_list_1";

        log("Category: waiting for suggestions #" + listId);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(listId)));

        By options = By.cssSelector("#" + listId + " [role='option']");
        List<WebElement> items = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(options));
        if (items.isEmpty()) throw new TimeoutException("No autocomplete options found in #" + listId);

        log("Category: clicking first option");
        safeClick(items.get(0));
        nap(150);
    }

    private void chooseFirstSubcategory() {
        By subcatLabels = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(subcatLabels));
        if (labels.isEmpty()) throw new RuntimeException("No subcategories found.");
        safeClick(labels.get(0));
        nap(150);
    }

    private void waitForSubcategoryChecked() {
        log("Subcat: waiting for checked radio");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[name='type_of_service']:checked")));
    }

    /* ================= Step 2: Date & Time ================= */

    private void waitForDateStep() {
        log("Step 2: waiting for date input");
        WebElement el = findVisible(By.cssSelector("input[name='start_date']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        nap(150);
    }

    private void setDateByJS(String ddMMyyyyWithDashes) {
        log("Date(JS): " + ddMMyyyyWithDashes);
        WebElement dateInput = findVisible(By.cssSelector("input[name='start_date']"));
        String js = "const el=arguments[0],v=arguments[1];el.value=v;el.setAttribute('value',v);" +
                "['input','change','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true})));";
        ((JavascriptExecutor) driver).executeScript(js, dateInput, ddMMyyyyWithDashes);
        String got = dateInput.getAttribute("value");
        if (got == null || !got.trim().equals(ddMMyyyyWithDashes)) {
            throw new RuntimeException("Date not set: " + got);
        }
    }

    private void pickTimeByText(String timeTextDesired) {
        log("Time: open dropdown");
        WebElement trigger = findVisible(By.id("customDropdownButton"));
        safeClick(trigger);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dropdown-menu.show, [role='menu'].show, .time-menu.show, .time-list.show")));
        List<WebElement> selects = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//button[normalize-space()='Select']")));
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

    private void setTimeByJS(String timeText) {
        log("Time(JS): " + timeText);
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
        try { new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".dropdown-menu.show,[role='menu'].show,.time-menu.show,.time-list.show"))); } catch (TimeoutException ignored) {}
    }

    private ExpectedCondition<Boolean> elementIsEnabled(WebElement el) {
        return d -> el != null && el.isDisplayed() && el.isEnabled()
                && !"true".equalsIgnoreCase(el.getAttribute("aria-disabled"))
                && (el.getAttribute("disabled") == null)
                && !(String.valueOf(el.getAttribute("class")).toLowerCase().contains("disabled"));
    }

    private WebElement findNextLikeButtonOrNull() {
        By[] locs = { By.xpath("//button[.//span[normalize-space()='Next']]"), By.xpath("//button[normalize-space()='Next']"), By.cssSelector("button.btn-primary-theme-quote[type='submit']"), By.cssSelector("button[type='submit']:not([disabled])") };
        for (By by : locs) { for (WebElement el : driver.findElements(by)) if (el.isDisplayed()) return el; }
        return null;
    }

    private void clickNextSmart() {
        closeOpenMenusIfAny(); waitForOverlayToDisappear();
        WebElement next = findNextLikeButtonOrNull();
        if (next == null) { ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);"); nap(120); next = findNextLikeButtonOrNull(); }
        if (next == null) { ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);"); nap(120); next = findNextLikeButtonOrNull(); }
        if (next == null) throw new TimeoutException("Next not found");
        new WebDriverWait(driver, Duration.ofSeconds(8)).until(elementIsEnabled(next));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", next);
        safeClick(next); waitForOverlayToDisappear();
    }

    /* =============== Address selection =============== */

    private List<WebElement> findAddressOptionsForInput(WebElement input) {
        String listId = input.getAttribute("aria-controls");
        if (listId != null && !listId.isBlank()) {
            List<WebElement> inside = driver.findElements(By.cssSelector("#" + listId + " [role='option'], #" + listId + " li, #" + listId + " .list-group-item, #" + listId + " .autocomplete-item"));
            if (!inside.isEmpty()) return inside;
        }
        try { WebElement cont = input.findElement(By.xpath("(.//following::ul[contains(@class,'autocomplete')][1] | .//following::div[contains(@class,'autocomplete')][1] | .//following::div[contains(@class,'dropdown-menu')][1] | .//following::ul[contains(@class,'dropdown-menu')][1])")); List<WebElement> rows = cont.findElements(By.cssSelector("[role='option'], li, .list-group-item, .autocomplete-item, .dropdown-item")); if (!rows.isEmpty()) return rows; } catch (NoSuchElementException ignored) {}
        List<WebElement> pac = driver.findElements(By.cssSelector(".pac-container .pac-item"));
        if (!pac.isEmpty()) return pac;
        return driver.findElements(By.cssSelector(".dropdown-menu.show .dropdown-item, .dropdown-menu.show li, [role='listbox'] [role='option']"));
    }

    private String selectAddressByIndex(String inputId, String query, int n) {
        if (n < 1) n = 1;
        WebElement input = findVisible(By.id(inputId));
        safeClick(input); input.clear(); input.sendKeys(query);

        List<WebElement> options = findAddressOptionsForInput(input);
        long end = System.currentTimeMillis() + 8000;
        while (options.isEmpty() && System.currentTimeMillis() < end) { nap(120); options = findAddressOptionsForInput(input); }
        if (options.isEmpty()) throw new TimeoutException("No address suggestions for: " + query);

        int idx = Math.min(n - 1, options.size() - 1);
        WebElement option = options.get(idx);

        WebElement clickable = null;
        try { clickable = option.findElement(By.cssSelector("a, button")); } catch (NoSuchElementException ignored) {}
        WebElement target = (clickable != null ? clickable : option);

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", target);
        safeClick(target);

        wait.until(d -> { String v = input.getAttribute("value"); return v != null && v.trim().length() > 3; });
        return input.getAttribute("value");
    }

    private String selectPickupByIndex(String q, int n) { return selectAddressByIndex("fetch_postcode", q, n); }
    private String selectDropByIndex(String q, int n)   { return selectAddressByIndex("fetch_address",  q, n); }

    /* ================= Step 3: Customer Details ================= */

    private void clearAndType(WebElement el, String text) {
        el.click(); el.sendKeys(Keys.chord(Keys.CONTROL,"a")); el.sendKeys(Keys.BACK_SPACE); el.clear(); el.sendKeys(text);
    }

    private boolean tickTermsSmartWithIframeSearch() {
        String script = "let cb=document.querySelector(\"input[type='checkbox'][name='terms']\");" +
                "if(!cb) cb=Array.from(document.querySelectorAll(\"input[type='checkbox']\")).find(c=>(c.name||'').toLowerCase().includes('term')||(c.id||'').toLowerCase().includes('term'));" +
                "if(cb){cb.checked=true;['input','change','click','blur'].forEach(e=>cb.dispatchEvent(new Event(e,{bubbles:true})));return true;} return false;";
        Object ok = ((JavascriptExecutor) driver).executeScript(script);
        if(Boolean.TRUE.equals(ok)) return true;

        List<WebElement> frames = driver.findElements(By.tagName("iframe"));
        for (int i=0;i<frames.size();i++) {
            try { driver.switchTo().frame(i); ok=((JavascriptExecutor) driver).executeScript(script); if(Boolean.TRUE.equals(ok)) { driver.switchTo().defaultContent(); return true; } }
            catch(Exception ignored) {} finally { try { driver.switchTo().defaultContent(); } catch(Exception ignored) {} }
        }
        return false;
    }

    private void fillCustomerDetails(String first,String last,String email,String phone,boolean agreeTerms) {
        log("Step3: fill details");
        WebElement firstName = findVisible(By.cssSelector("input[name='first_name']"));
        WebElement lastName  = findVisible(By.cssSelector("input[name='sur_name']"));
        WebElement emailEl   = findVisible(By.cssSelector("input[name='email']"));
        WebElement phoneEl   = findVisible(By.cssSelector("input#phone_[name='phone']"));

        clearAndType(firstName, first);
        clearAndType(lastName, last);
        clearAndType(emailEl, email);
        clearAndType(phoneEl, phone);
        ((JavascriptExecutor) driver).executeScript("['input','change','blur'].forEach(e=>arguments[0].dispatchEvent(new Event(e,{bubbles:true})));", phoneEl);

        if(agreeTerms) { boolean ticked = tickTermsSmartWithIframeSearch(); if(!ticked) throw new TimeoutException("Could not find Terms checkbox."); closeUnexpectedTabsExcept(mainHandle); }
    }

    private void clickSubmit() {
        log("Step3: click Submit");
        By[] candidates = new By[] { By.cssSelector("button.next[type='submit']"), By.xpath("//button[@type='submit' and (contains(.,'Submit') or contains(.,'SUBMIT'))]") };
        WebElement btn = null;
        for(By by:candidates){ List<WebElement> found=driver.findElements(by); if(!found.isEmpty() && found.get(0).isDisplayed()){ btn=found.get(0); break; } }
        if(btn==null) throw new NoSuchElementException("Submit button not found");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        wait.until(ExpectedConditions.elementToBeClickable(btn));
        safeClick(btn);
        waitForOverlayToDisappear();
        closeUnexpectedTabsExcept(mainHandle);
    }

    /* ===================== Full Flow ===================== */

    private void runFlow(String category, String date, String time,
                         String pickupQuery, int pickupIdx,
                         String dropQuery, int dropIdx,
                         String first, String last, String email, String phone) {
        chooseCategory(category);
        chooseFirstSubcategory();
        waitForSubcategoryChecked();
        clickNextSmart();
        waitForDateStep();
        setDateByJS(date);
        try { pickTimeByText(time); } catch(Exception e) { setTimeByJS(time); }
        selectPickupByIndex(pickupQuery, pickupIdx);
        selectDropByIndex(dropQuery, dropIdx);
        clickNextSmart();
        fillCustomerDetails(first,last,email,phone,true);
        clickSubmit();
    }

  //  @Test
    public void testRentXQuoteFlow() {
        test = extent.createTest("RentX Quote Flow Test");
        driver.get("https://rentx.com/quote");
        runFlow(
                "l", "19-09-2025", "01:00 PM",
                "SW9 8TP", 1,
                "SW9 8TP", 1,
                "John", "Doe", "vel.com", "9876543210"
        );
    }
}
