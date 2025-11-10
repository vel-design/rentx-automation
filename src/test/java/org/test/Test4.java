package org.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.time.Duration;

@Listeners(org.test.TestListener.class)
public class Test4 {

    private WebDriver driver;

    @BeforeClass
    public void setUp() {
        ChromeOptions options = new ChromeOptions();

        // ✅ Run headless in CI (GitHub Actions) but visible locally
        String ciEnv = System.getenv("CI");
        if (ciEnv != null && ciEnv.equalsIgnoreCase("true")) {
            options.addArguments("--headless=new");
        }

        // ✅ Always recommended for Linux/CI
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        // ✅ Let Selenium auto-detect Chrome binary (no hardcoded path)
        driver = new ChromeDriver(options);
        TestListener.driver = driver;
    }

    @Test
    public void rentXQuoteWorkflow() throws InterruptedException {
        driver.manage().window().maximize();
        driver.get("https://rentx.com/quote");
        Test4Steps steps = new Test4Steps(driver);

        String mainHandle = driver.getWindowHandle();
        steps.runFlowWithAddressIndices(
                "l",
                "19-12-2025",
                "01:00 PM",
                "st", 1,
                "12", 2
        );

        steps.waitForStep3();
        steps.fillCustomerDetails("Test Vel", "Test Mobo", "test@mobo.com", "08754140966", true, mainHandle);
        steps.clickSubmit(mainHandle);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement thankYouMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("h3.ticket-text.ticket-text-color.mb-2.pb-2.border-bottom")
        ));

        assertTrue(thankYouMessage.getText().contains("Thank you for the"),
                "Thank you page text mismatch or not found!");
        assertTrue(driver.getPageSource().contains("Thank you for the"),
                "Thank you page not displayed or text mismatch!");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

