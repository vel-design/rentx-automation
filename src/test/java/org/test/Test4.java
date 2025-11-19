package org.test;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;

@Listeners(org.test.TestListener.class)
public class Test4 {

    private WebDriver driver;
    private Test4Steps steps;
    private String mainHandle;

    @BeforeClass
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        boolean isCI = "true".equalsIgnoreCase(System.getenv("CI"));

        if (isCI) {
            System.out.println("Running in CI environment → enabling headless Chrome.");
            options.addArguments("--headless=new");
        } else {
            System.out.println("Running locally → headless mode disabled.");
        }

        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--window-size=1920,1080", "--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        TestListener.driver = driver;
        steps = new Test4Steps(driver);
    }

    @Test
    public void rentXQuoteWorkflow() throws IOException {
        System.out.println(" Opening RentX quote page...");
        driver.get("https://rentx.com/quote");

        mainHandle = driver.getWindowHandle();

        try {
            System.out.println("🔹 Running booking flow steps...");
            steps.runFlowWithAddressIndices(
                    "l",             // search term (category)
                    "19-12-2026",    // date
                    "01:00 PM",      // time
                    "st", 1,         // pickup
                    "12", 2          // drop
            );

            steps.waitForStep3();

            System.out.println(" Filling customer details...");
            steps.fillCustomerDetails("Test Vel", "Test Mobo", "test@mobo.com", "8888888888", true, mainHandle);

            System.out.println(" Submitting quote form...");
            steps.clickSubmit(mainHandle);

            // ✅ Wait dynamically for Thank You page
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
            WebElement thankYouMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("h3.ticket-text.ticket-text-color.mb-2.pb-2.border-bottom")
            ));

            String text = thankYouMsg.getText();
            Assert.assertTrue(text.contains("Thank you for the"), 
                    "Expected thank you message not found! Actual: " + text);

            System.out.println(" Test passed — Thank you message verified successfully!");

        } catch (Exception e) {
            takeScreenshot("failure_rentx_quote.png");
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            Assert.fail("Test failed due to: " + e.getMessage());
        }
    }

    private void takeScreenshot(String fileName) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File("test-output/screenshots/" + fileName);
            Files.createDirectories(dest.getParentFile().toPath());
            Files.copy(screenshot.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" Screenshot saved: " + dest.getAbsolutePath());
        } catch (IOException ex) {
            System.err.println(" Failed to capture screenshot: " + ex.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            System.out.println(" Browser closed. Test completed.");
        }
    }
}
