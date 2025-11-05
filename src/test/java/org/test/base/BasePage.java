package org.test.base;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;

public class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.js     = (JavascriptExecutor) driver;
    }

    // ----- Helpers -----
    protected void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    protected void debug(String msg) {
        System.out.println("[DEBUG] " + msg);
    }

    protected void safeClick(By locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception e) {
            WebElement el = driver.findElement(locator);
            js.executeScript("arguments[0].scrollIntoView(true); arguments[0].click();", el);
        }
    }

    protected WebElement findVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected List<WebElement> findAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected void waitForOverlayToDisappear() {
        try {
            By overlay = By.cssSelector(".loading-overlay, .spinner-border");
            wait.until(ExpectedConditions.invisibilityOfElementLocated(overlay));
        } catch (Exception ignored) {}
    }

    protected void closeUnexpectedTabsExcept(String expectedUrl) {
        String original = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (!driver.getCurrentUrl().contains(expectedUrl)) {
                driver.close();
            }
        }
        driver.switchTo().window(original);
    }

    protected String normalizeTime(String input) {
        input = input.trim().toUpperCase();
        if (!input.contains("AM") && !input.contains("PM")) input += " AM";
        return input.replaceAll("\\s+", " ");
    }

    protected void clickNextSmart() {
        By nextBtn = By.cssSelector("button.btn-primary-theme-quote[type='submit']");
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(nextBtn));
            js.executeScript("arguments[0].scrollIntoView(true);", btn);
            btn.click();
            debug("Clicked Next button normally.");
        } catch (Exception e) {
            debug("Next button click failed, retrying via JS...");
            WebElement btn = driver.findElement(nextBtn);
            js.executeScript("arguments[0].click();", btn);
        }
        waitForOverlayToDisappear();
    }
}
