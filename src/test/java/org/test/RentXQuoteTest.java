package org.test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.annotations.Test;


import java.time.Duration;

public class RentXQuoteTest {

    private WebDriver driver;
    private RentXFlow flow;

    @BeforeClass
    public void setup() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        flow = new RentXFlow(driver);
    }

    @Test
    public void testQuoteFlow() throws InterruptedException {
        driver.get("https://rentx.com/quote"); // 🔹 replace with actual URL

        // Step 1 & 2
        flow.runFlowWithAddressIndices(
                "l",              // category
                "19-09-2025",     // date
                "01:00 PM",       // time
                "st", 1,          // pickup
                "12", 2           // drop
        );

        // Step 3
        flow.waitForStep3();
        flow.fillCustomerDetails(
                "Test",
                "Vel",
                "test@mobo.com",
                "08754140962",
                true
        );

        flow.clickSubmit();

        // 🔹 Assertion example
        Assert.assertTrue(driver.getPageSource().contains("Thank you") ||
                          driver.getPageSource().contains("Success"),
                          "Form submission may have failed!");
    }

    @AfterClass
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
