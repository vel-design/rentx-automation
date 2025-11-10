package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;
import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

@Listeners(org.test.TestListener.class)
public class Test4 {

    private WebDriver driver;

    @BeforeClass
    public void setUp() {
        ChromeOptions options = new ChromeOptions();

        // ✅ Detect if running in GitHub Actions or local
        boolean isCI = "true".equalsIgnoreCase(System.getenv("CI"));

        if (isCI) {
            System.out.println("Running in CI environment → enabling headless Chrome.");
            options.addArguments("--headless=new");
        } else {
            System.out.println("Running locally → headless mode disabled.");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        TestListener.driver = driver;
    }

    @Test
    public void rentXQuoteWorkflow() throws InterruptedException, IOException {
        System.out.println("🔹 Opening RentX quote page...");
        driver.get("https://rentx.com/quote");

        Test4Steps steps = new Test4Steps(driver);
        String mainHandle = driver.getWindowHandle();

        System.out.println("🔹 Running booking steps...");
        steps.runFlowWithAddressIndices(
                "l",
                "19-12-2025",
                "01:00 PM",
                "st", 1,
                "12", 2
        );

        steps.waitForStep3();

        System.out.println("🔹 Filling customer details...");
        steps.fillCustomerDetails("Test Vel", "Test Mobo", "test@mobo.com", "8888888888", true, mainHandle);

        System.out.println("🔹 Submitting quote form...");
        steps.clickSubmit(mainHandle);

        // ✅ Wait for "Thank you" page (extended wait for CI)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        try {
            WebElement thankYouMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("h3.ticket-text.ticket-text-color.mb-2.pb-2.border-bottom")
            ));

            assertTrue(thankYouMessage.getText().contains("Thank you for the"),
                    "Thank you page text mismatch or not found!");
            assertTrue(driver.getPageSource().contains("Thank you for the"),
                    "Thank you page not displayed or text mismatch!");

            System.out.println("✅ Test passed: Thank you page verified successfully.");

        } catch (TimeoutException e) {
            takeScreenshot("failure_thankyou_not_found.png");
            System.err.println("❌ Timeout waiting for Thank You message. Screenshot captured.");
            throw e;
        }
    }

    @AfterMethod
    public void captureFailureScreenshot(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            try {
                takeScreenshot(result.getName() + "_failed.png");
                System.err.println("⚠️ Failure screenshot saved for: " + result.getName());
            } catch (IOException e) {
                System.err.println("❌ Failed to save screenshot: " + e.getMessage());
            }
        }
    }

    private void takeScreenshot(String fileName) throws IOException {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File dest = new File("test-output/screenshots/" + fileName);
        Files.createDirectories(dest.getParentFile().toPath());
        Files.copy(screenshot.toPath(), dest.toPath());
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println("🧹 Browser closed. Test completed.");
        }
    }
}
