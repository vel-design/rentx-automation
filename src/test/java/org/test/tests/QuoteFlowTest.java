package org.test.tests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.*;
import org.test.pages.*;
import org.test.base.*;

public class QuoteFlowTest {
    private WebDriver driver;

    @BeforeClass
    public void setup() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://rentx.com/quote");
    }

    @Test
    public void bookingFlow() {
        CategoryPage category = new CategoryPage(driver);
        BookingPage booking   = new BookingPage(driver);
        CustomerPage customer = new CustomerPage(driver);

        // Step 1: Category
        category.chooseCategory("l");
        category.chooseFirstSubcategory();
        category.waitForSubcategoryChecked();
        category.goNext();

        // Step 2: Booking details
        booking.setDate("19-09-2025");
        booking.setTime("01:00 PM");
        booking.selectPickupByIndex("st", 0);
        booking.selectDropByIndex("12", 0);
        booking.goNext();

        // Step 3: Customer details
        customer.fillCustomerDetails("Test Vel", "Test Mobo", "test@mobo.com", "08754140962");
        customer.tickTermsSmartWithIframeSearch();
        customer.submit();
    }

    @AfterClass
    public void teardown() {
        if (driver != null) driver.quit();
    }
}
