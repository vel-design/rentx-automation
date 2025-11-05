package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class Test {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public Test(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    /* -------------------- Helpers -------------------- */

    /** Scrolls element into view and tries normal click; falls back to JS click if intercepted. */
    private void safeClick(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (ElementClickInterceptedException | TimeoutException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    /** Find element safely and return it (presence + visibility). */
    private WebElement findVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Small utility sleep (use sparingly). */
    private static void nap(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    /* -------------------- Steps -------------------- */

    /** Step 1: Type term and select FIRST suggestion from autocomplete */
    public void chooseCategory(String searchTerm) {
        WebElement categoryInput = findVisible(By.id("fetch_service"));
        safeClick(categoryInput);      // focus input
        categoryInput.clear();
        categoryInput.sendKeys(searchTerm);

        // Wait for suggestions container and click first option
        By suggestionList = By.id("autoComplete_list_1");
        wait.until(ExpectedConditions.visibilityOfElementLocated(suggestionList));

        By options = By.cssSelector("#autoComplete_list_1 [role='option']");
        List<WebElement> items = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(options));
        WebElement first = items.get(0);

        safeClick(first);              // robust click on first suggestion
        nap(150);                      // tiny debounce if UI animates
    }

    /** Step 2A: Pick FIRST subcategory (radio + label) */
    public void chooseFirstSubcategory() {
        By subcatLabels = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(subcatLabels));
        if (labels.isEmpty()) throw new RuntimeException("No subcategories found.");
        safeClick(labels.get(0));
        nap(150);
    }

    /** Step 2B: Pick subcategory by (partial or exact) visible text */
    public void chooseSubcategoryByText(String subcatText) {
        By subcatLabels = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(subcatLabels));

        for (WebElement label : labels) {
            String txt = label.getText().trim();
            if (txt.equalsIgnoreCase(subcatText) || txt.toLowerCase().contains(subcatText.toLowerCase())) {
                safeClick(label);
                nap(150);
                return;
            }
        }
        throw new RuntimeException("Subcategory not found: " + subcatText);
    }

    /** Step 3: Click the Next button you provided */
    public void clickNext() {
        By nextBtn = By.xpath("//button[.//span[normalize-space()='Next']]");
        WebElement next = wait.until(ExpectedConditions.presenceOfElementLocated(nextBtn));
        safeClick(next);
    }

    /** Convenience flow: Category -> first Subcategory -> Next */
    public void runFlow(String term) {
        chooseCategory(term);
        chooseFirstSubcategory();
        clickNext();
    }

    /* -------------------- Runnable Demo -------------------- */

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        try {
            driver.get("https://rentx.com/quote"); // TODO: replace with your env if needed

            Test selector = new Test(driver);

            // Example: type "l" -> select first suggestion -> first subcategory -> Next
            selector.runFlow("l");

            // Or pick a specific subcategory by text:
            // selector.chooseCategory("b");
            // selector.chooseSubcategoryByText("Up to 25 Passenger Charter Buses");
             selector.clickNext();

        } finally {
            driver.quit();
        }
    }
}
