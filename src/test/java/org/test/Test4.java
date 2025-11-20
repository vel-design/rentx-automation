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

@BeforeClass(alwaysRun = true)
public void setUp() {
    ChromeOptions options = new ChromeOptions();
    boolean isCI = "true".equalsIgnoreCase(System.getenv("CI"));

    // Recommended stability flags (works locally & in CI)
    options.addArguments("--headless=new");
    options.addArguments("--disable-gpu");
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--window-size=1920,1080");
    options.addArguments("--remote-allow-origins=*");

    if (isCI) {
        System.out.println("Running in CI environment → Headless enabled.");
    } else {
        System.out.println("Running locally.");
    }

    driver = new ChromeDriver(options);
    TestListener.driver = driver;
    steps = new Test4Steps(driver);
}

@Test
public void rentXQuoteWorkflow() {
    System.out.println("Opening RentX quote page...");
    driver.get("https://rentx.com/quote");

    mainHandle = driver.getWindowHandle();

    try {
        debug("Booking flow started");

        // Execute full booking flow
        steps.runFlowWithAddressIndices(
                "l",
                "19-12-2026",
                "01:00 PM",
                "st", 1,
                "12", 2
        );

        // Wait for customer fields
        steps.waitForStep3();

        System.out.println("Filling customer details...");
        steps.fillCustomerDetails(
                "Test Vel",
                "Test Mobo",
                "vel@mobo.co.uk",
                "8888888888",
                true,
                mainHandle
        );

        System.out.println("Submitting quote...");
        steps.clickSubmit(mainHandle);

        // Wait for Thank You confirmation
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        WebElement thankYouMsg = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("h3.ticket-text.ticket-text-color.mb-2.pb-2.border-bottom")
                )
        );

        String text = thankYouMsg.getText();

        Assert.assertTrue(
                text.contains("Thank you for the"),
                "Expected thank you message not found! Actual: " + text
        );

        System.out.println("Test Passed — Thank You page located successfully!");

        // 🎯 Screenshot ONLY when Thank You is visible
        takeScreenshot("thank_you_visible.png");

        return;

    } catch (Exception e) {
        takeScreenshot("failure_rentx_quote.png");
        dumpPageSource();
        e.printStackTrace();
        Assert.fail("Test failed: " + e.getMessage());
    }
}

private void takeScreenshot(String fileName) {
    try {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File dest = new File("test-output/screenshots/" + fileName);
        Files.createDirectories(dest.getParentFile().toPath());
        Files.copy(screenshot.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Screenshot saved: " + dest.getAbsolutePath());
    } catch (IOException ex) {
        System.err.println("Screenshot capture failed: " + ex.getMessage());
    }
}

private void dumpPageSource() {
    try {
        String html = driver.getPageSource();
        String file = "test-output/screenshots/page-source-" + System.currentTimeMillis() + ".html";
        Files.createDirectories(Paths.get("test-output/screenshots"));
        Files.writeString(Paths.get(file), html);
        System.out.println("Page source saved: " + file);
    } catch (IOException ex) {
        System.err.println("Failed to save page source: " + ex.getMessage());
    }
}

private void debug(String message) {
    System.out.println("DEBUG: " + message + " | URL: " + driver.getCurrentUrl());
}

@AfterClass(alwaysRun = true)
public void tearDown() {
    if (driver != null) {
        driver.quit();
        System.out.println("Browser closed — Test Completed.");
    }
}


}
