package org.test.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.test.base.BasePage;

import java.util.List;

public class CustomerPage extends BasePage {
    public CustomerPage(WebDriver driver) {
        super(driver);
    }

    public void fillCustomerDetails(String first, String last, String email, String phone) {
        type(By.cssSelector("input[name='first_name']"), first);
        type(By.cssSelector("input[name='sur_name']"), last);
        type(By.cssSelector("input[name='email']"), email);
        type(By.cssSelector("input[name='phone']"), phone);
    }

    private void type(By locator, String text) {
        WebElement el = findVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    public void tickTermsSmartWithIframeSearch() {
        By termsSelector = By.cssSelector("input[type='checkbox'][name='terms']");

        try {
            WebElement cb = wait.until(ExpectedConditions.presenceOfElementLocated(termsSelector));
            if (!cb.isSelected()) cb.click();
            debug("Ticked terms checkbox normally.");
            return;
        } catch (Exception ignored) {}

        // Fallback: try iframes
        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        for (WebElement iframe : iframes) {
            driver.switchTo().frame(iframe);
            try {
                WebElement cb = driver.findElement(termsSelector);
                if (!cb.isSelected()) cb.click();
                debug("Ticked terms inside iframe.");
                driver.switchTo().defaultContent();
                return;
            } catch (Exception e) {
                driver.switchTo().defaultContent();
            }
        }

        throw new RuntimeException("Terms checkbox not found!");
    }

    public void submit() {
        By submitBtn = By.cssSelector("button.btn-primary-theme-quote.next[type='submit']");
        safeClick(submitBtn);
        waitForOverlayToDisappear();
    }
}
