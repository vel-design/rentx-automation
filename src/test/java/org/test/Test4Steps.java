package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.*;

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

    private void safeClick(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (ElementClickInterceptedException | TimeoutException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private WebElement findVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void waitForOverlayToDisappear() {
        By overlay = By.cssSelector(".loading-overlay, .spinner, .blockUI, .modal-backdrop.show, .overlay, .page-loader");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.invisibilityOfElementLocated(overlay));
        } catch (TimeoutException ignored) {}
    }

    // --- CATEGORY & SUBCATEGORY SELECTION ---

    public void chooseCategory(String searchTerm) {
        debug("Selecting category...");
        WebElement input = findVisible(By.id("fetch_service"));
        safeClick(input);
        input.clear();
        input.sendKeys(searchTerm);

        String listId = input.getAttribute("aria-controls");
        if (listId == null || listId.isBlank()) listId = "autoComplete_list_1";

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(listId)));
        List<WebElement> options = driver.findElements(By.cssSelector("#" + listId + " [role='option']"));
        if (options.isEmpty()) throw new TimeoutException("No autocomplete options for: " + searchTerm);

        safeClick(options.get(0));
        waitForOverlayToDisappear();
    }

    public void chooseFirstSubcategory() {
        debug("Choosing subcategory...");
        List<WebElement> labels = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("label[for^='type_of_service']")));
        if (labels.isEmpty()) throw new RuntimeException("No subcategories found!");
        safeClick(labels.get(0));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[name='type_of_service']:checked")));
    }

    // --- DATE & TIME SELECTION ---

    private void waitForDateStep() {
        debug("Waiting for date step...");
        WebElement el = findVisible(By.cssSelector("input[name='start_date']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    public void setDateByJS(String date) {
        debug("Setting date: " + date);
        WebElement dateInput = findVisible(By.cssSelector("input[name='start_date']"));
        ((JavascriptExecutor) driver).executeScript(
                "const el=arguments[0],v=arguments[1];el.value=v;el.setAttribute('value',v);" +
                        "['input','change','blur'].forEach(e=>el.dispatchEvent(new Event(e,{bubbles:true})));",
                dateInput, date);
    }

    public void pickTimeByText(String timeText) {
        debug("Selecting time: " + timeText);
        WebElement dropdown = findVisible(By.id("customDropdownButton"));
        safeClick(dropdown);

        List<WebElement> options = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.xpath("//button[normalize-space()='Select']")));
        for (WebElement btn : options) {
            String rowText = btn.getText().trim();
            if (rowText.contains(timeText)) {
                safeClick(btn);
                return;
            }
        }
        // fallback if not found
        safeClick(options.get(0));
    }

    // --- ADDRESS SELECTION ---

    public String selectAddressByIndex(String inputId, String query, int index) {
        WebElement input = findVisible(By.id(inputId));
        safeClick(input);
        input.clear();
        input.sendKeys(query);

        List<WebElement> options = new ArrayList<>();
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            options = driver.findElements(By.cssSelector(".pac-item, [role='option'], .dropdown-item"));
            if (!options.isEmpty()) break;
        }
        if (options.isEmpty()) throw new TimeoutException("No address suggestions for " + query);

        WebElement chosen = options.get(Math.min(index - 1, options.size() - 1));
        safeClick(chosen);

        wait.until(d -> input.getAttribute("value") != null && input.getAttribute("value").length() > 3);
        return input.getAttribute("value");
    }

    public String selectPickupByIndex(String q, int n) { return selectAddressByIndex("fetch_postcode", q, n); }
    public String selectDropByIndex(String q, int n)   { return selectAddressByIndex("fetch_address", q, n); }

    // --- STEP 3: CUSTOMER DETAILS ---

    public void waitForStep3() {
        debug("Waiting for Step 3...");
        WebElement firstNameInput = findVisible(By.cssSelector("input[name='first_name']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", firstNameInput);
        System.out.println("✅ Step 3 form visible");
    }

    private void clearAndType(WebElement el, String value) {
        wait.until(ExpectedConditions.visibilityOf(el));
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        el.clear();
        el.sendKeys(value);
    }

    private void tickTermsIfPresent() {
        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox'][name*='term']"));
        if (!checkboxes.isEmpty()) {
            WebElement cb = checkboxes.get(0);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", cb);
            if (!cb.isSelected()) cb.click();
        }
    }

    public void fillCustomerDetails(String first, String last, String email, String phone, boolean agree, String mainHandle) {
        debug("Filling customer form...");
        WebElement firstName = findVisible(By.cssSelector("input[name='first_name']"));
        WebElement lastName = findVisible(By.cssSelector("input[name='sur_name']"));
        WebElement emailEl = findVisible(By.cssSelector("input[name='email']"));
        WebElement phoneEl = findVisible(By.cssSelector("input[name='phone'], input[id*='phone']"));

        clearAndType(firstName, first);
        clearAndType(lastName, last);
        clearAndType(emailEl, email);
        clearAndType(phoneEl, phone);

        if (agree) tickTermsIfPresent();
        driver.switchTo().window(mainHandle);
    }

    // --- SUBMISSION ---

    public void clickSubmit(String mainHandle) {
        debug("Submitting form...");
        WebElement submitBtn = findVisible(By.cssSelector("button[type='submit']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        safeClick(submitBtn);
        waitForOverlayToDisappear();
        driver.switchTo().window(mainHandle);
    }

    // --- FLOW EXECUTION ---

    public void runFlowWithAddressIndices(String term,
                                          String date, String time,
                                          String pickup, int pIndex,
                                          String drop, int dIndex) {
        chooseCategory(term);
        chooseFirstSubcategory();
        clickNextSmart();
        waitForDateStep();
        setDateByJS(date);
        try {
            pickTimeByText(time);
        } catch (Exception e) {
            debug("Time dropdown fallback → using JS input");
            setDateByJS(time);
        }
        selectPickupByIndex(pickup, pIndex);
        selectDropByIndex(drop, dIndex);
        clickNextSmart();
    }

    private void clickNextSmart() {
        waitForOverlayToDisappear();
        WebElement nextBtn = findVisible(By.cssSelector("button.btn-primary-theme-quote, button.next, button[type='submit']"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", nextBtn);
        safeClick(nextBtn);
        waitForOverlayToDisappear();
    }
}
