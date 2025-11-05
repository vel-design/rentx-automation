package org.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class CategorySelector {

    private WebDriver driver;
    private WebDriverWait wait;

    // Constructor
    public CategorySelector(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void chooseCategory(String searchTerm) {
        // Step 1: type into input
        WebElement categoryInput = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("fetch_service"))
        );
        categoryInput.click();
        categoryInput.clear();
        categoryInput.sendKeys(searchTerm);

        // Step 2: wait for dropdown
        By suggestionList = By.id("autoComplete_list_1");
        wait.until(ExpectedConditions.visibilityOfElementLocated(suggestionList));

        // Step 3: select first option
        By options = By.cssSelector("#autoComplete_list_1 [role='option']");
        List<WebElement> items = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(options)
        );
        items.get(0).click();
    }

    // 👉 MAIN METHOD
    public static void main(String[] args) {
        // Setup WebDriver
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://rentx.com/quote"); // <-- replace with your real page URL

        // Create instance and run the method
        CategorySelector selector = new CategorySelector(driver);
        selector.chooseCategory("t"); // Try with "l" → picks first suggestion (Limo Rental)
       // selector.chooseCategory("b"); // Try with "b" → picks first suggestion for "b"

        // Close the browser after test
        driver.quit();
    }
}
