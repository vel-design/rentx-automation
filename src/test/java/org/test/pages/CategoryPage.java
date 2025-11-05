package org.test.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.test.base.BasePage;

import java.util.List;
import java.util.Optional;

public class CategoryPage extends BasePage {
    public CategoryPage(WebDriver driver) {
        super(driver);
    }

    public void chooseCategory(String term) {
        WebElement input = findVisible(By.id("fetch_service"));
        input.clear();
        input.sendKeys(term);

        String listId = Optional.ofNullable(input.getAttribute("aria-controls"))
                                .orElse("autoComplete_list_1");

        By options = By.cssSelector("#" + listId + " [role='option']");
        List<WebElement> items = findAllVisible(options);
        items.get(0).click();
    }

    public void chooseFirstSubcategory() {
        By subcatLabels = By.cssSelector("label[for^='type_of_service']");
        List<WebElement> labels = findAllVisible(subcatLabels);
        labels.get(0).click();
    }

    public void waitForSubcategoryChecked() {
        By subcat = By.cssSelector("input[id^='type_of_service']:checked");
        wait.until(ExpectedConditions.presenceOfElementLocated(subcat));
    }

    public void goNext() {
        clickNextSmart();
    }
}
