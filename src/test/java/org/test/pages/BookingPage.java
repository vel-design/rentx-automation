package org.test.pages;

import org.openqa.selenium.*;
import org.test.base.BasePage;

import java.util.List;

public class BookingPage extends BasePage {
    public BookingPage(WebDriver driver) {
        super(driver);
    }

    public void setDate(String date) {
        WebElement dateInput = findVisible(By.cssSelector("input[name='start_date']"));
        js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
                dateInput, date);
    }

    public void setTime(String time) {
        WebElement timeInput = findVisible(By.cssSelector("input[name='start_time']"));
        js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
                timeInput, normalizeTime(time));
    }

    public void selectPickupByIndex(String query, int index) {
        selectAddress("fetch_postcode", query, index);
    }

    public void selectDropByIndex(String query, int index) {
        selectAddress("fetch_address", query, index);
    }

    private void selectAddress(String id, String query, int index) {
        WebElement input = findVisible(By.id(id));
        input.clear();
        input.sendKeys(query);

        By options = By.cssSelector(".pac-container .pac-item, .dropdown-menu.show .dropdown-item");
        List<WebElement> list = findAllVisible(options);

        if (index >= 0 && index < list.size()) {
            list.get(index).click();
        } else {
            list.get(0).click();
        }
    }

    public void goNext() {
        clickNextSmart();
    }
}
