package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

public class RentXFlow {

    private final WebDriver driver;
    private final WebDriverWait wait;
    public RentXFlow(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        new Actions(driver);
    }

    /* -----------------
       STEP 1 & 2
    ----------------- */
    public void runFlowWithAddressIndices(
            String categorySearch,
            String date,
            String time,
            String pickupLocation,
            int pickupIndex,
            String dropLocation,
            int dropIndex
    ) {
        selectCategoryAndSubcategory(categorySearch);
        selectDateAndTime(date, time);
        selectAddressByIndex("pickup", pickupLocation, pickupIndex);
        selectAddressByIndex("drop", dropLocation, dropIndex);
        clickNextSmart();
    }

    private void selectCategoryAndSubcategory(String searchText) {
        WebElement searchBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("fetch_service"))
        );
        searchBox.clear();
        searchBox.sendKeys(searchText);

        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(".pac-item"), 0));
        driver.findElements(By.cssSelector(".pac-item")).get(0).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".sub_category a"))).click();
    }

    private void selectDateAndTime(String date, String time) {
        WebElement dateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("fetch_date"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].value='" + date + "';", dateInput);

        WebElement timeInput = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("fetch_time"))
        );
        try {
            Select select = new Select(timeInput);
            select.selectByVisibleText(time);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].value='" + time + "';", timeInput);
        }
    }

    private void selectAddressByIndex(String type, String query, int index) {
        By inputLocator = type.equals("pickup") ? By.id("fetch_postcode") : By.id("fetch_address");
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
        input.clear();
        input.sendKeys(query);

        try {
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(".pac-item"), index));
            driver.findElements(By.cssSelector(".pac-item")).get(index).click();
        } catch (Exception e) {
            // fallback
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("ul li"), index));
            driver.findElements(By.cssSelector("ul li")).get(index).click();
        }
    }

    private void clickNextSmart() {
        By[] locators = {
                By.cssSelector(".btn-next"),
                By.xpath("//button[contains(.,'Next')]"),
                By.cssSelector("#next-button"),
                By.cssSelector("button[type='submit']")
        };

        for (By locator : locators) {
            try {
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(locator));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextBtn);
                nextBtn.click();
                return;
            } catch (Exception ignored) {}
        }
    }

    /* -----------------
       STEP 3
    ----------------- */
    public void waitForStep3() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("first_name")));
    }

    public void fillCustomerDetails(
            String name,
            String surname,
            String email,
            String phone,
            boolean terms
    ) throws InterruptedException {
        WebElement firstName = driver.findElement(By.id("first_name"));
        firstName.clear();
        firstName.sendKeys(name);

        WebElement lastName = driver.findElement(By.id("surname"));
        lastName.clear();
        lastName.sendKeys(surname);

        WebElement emailInput = driver.findElement(By.id("email"));
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement phoneInput = driver.findElement(By.id("phone"));
        phoneInput.clear();
        phoneInput.sendKeys(phone);

        if (terms) {
            try {
                WebElement termsCheckbox = driver.findElement(By.cssSelector(".checkbox-agree input"));
                if (!termsCheckbox.isSelected()) {
                    termsCheckbox.click();
                }
            } catch (Exception ignored) {}
        }
    }

    public void clickSubmit() {
        By[] locators = {
                By.cssSelector("#submit-button"),
                By.xpath("//button[contains(.,'Submit')]"),
                By.cssSelector("button[type='submit']")
        };

        for (By locator : locators) {
            try {
                WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(locator));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
                submitBtn.click();
                return;
            } catch (Exception ignored) {}
        }
    }
}
