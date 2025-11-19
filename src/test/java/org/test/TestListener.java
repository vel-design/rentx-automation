package org.test;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.*;
import org.testng.asserts.SoftAssert;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class TestListener implements ITestListener {

    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();
    public static WebDriver driver;

    public static ThreadLocal<SoftAssert> softAssert = new ThreadLocal<>();

    @Override
    public void onStart(ITestContext context) {
        ExtentSparkReporter reporter = new ExtentSparkReporter("test-output/ExtentReport.html");
        reporter.config().setDocumentTitle("Automation Test Report");
        reporter.config().setReportName("Regression Suite");
        reporter.config().setTheme(Theme.DARK);

        extent = new ExtentReports();
        extent.attachReporter(reporter);
        extent.setSystemInfo("Tester", "QA Team");
        extent.setSystemInfo("Environment", System.getProperty("env", "Production"));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
        extent.setSystemInfo("Build Number", System.getenv("BUILD_NUMBER") == null ? "N/A" : System.getenv("BUILD_NUMBER"));
    }

    @Override
    public void onTestStart(ITestResult result) {
        test.set(extent.createTest(result.getMethod().getMethodName()));
        softAssert.set(new SoftAssert());
        // Log parameters
        Object[] params = result.getParameters();
        for (Object param : params) {
            test.get().assignCategory(String.valueOf(param));
        }
        logStep("Test started");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        waitUntilThankYouPageLoads();
        String screenshotPath = captureFullPageScreenshot(result.getName());
        if (screenshotPath != null) {
            test.get().pass("Test Passed")
                .addScreenCaptureFromPath(screenshotPath);
        } else {
            test.get().pass("Test Passed (No screenshot)");
        }
        logStep("Test passed");
        try {
            SoftAssert sa = softAssert.get();
            if (sa != null) sa.assertAll();
        } catch (AssertionError ae) {
            test.get().fail("Soft assertions failed: " + ae.getMessage());
        }
        softAssert.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String screenshotPath = captureFullPageScreenshot(result.getName());
        if (screenshotPath != null) {
            test.get().fail(result.getThrowable())
                .addScreenCaptureFromPath(screenshotPath);
        } else {
            test.get().fail(result.getThrowable());
        }
        logStep("Test failed");
        try {
            SoftAssert sa = softAssert.get();
            if (sa != null) sa.assertAll();
        } catch (AssertionError ae) {
            test.get().fail("Soft assertions failed: " + ae.getMessage());
        }
        softAssert.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        test.get().skip("Test Skipped");
        softAssert.remove();
    }

    @Override
    public void onFinish(ITestContext context) {
        extent.flush();
    }

    public void logStep(String message) {
        test.get().info(message);
        // To enable step screenshot:
        // String path = captureFullPageScreenshot("Step_" + System.currentTimeMillis());
        // if (path != null) test.get().addScreenCaptureFromPath(path, message);
    }

    // Full page screenshot using AShot 
    private String captureFullPageScreenshot(String testName) {
        if (driver == null) return null;
        try {
            Screenshot screenshot = new AShot().shootingStrategy(
                ru.yandex.qatools.ashot.shooting.ShootingStrategies.viewportPasting(200)).takeScreenshot(driver);
            BufferedImage image = screenshot.getImage();

            String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String relativePath = "screenshots/" + testName + "_" + date + ".png";
            String absolutePath = "test-output/" + relativePath;

            Files.createDirectories(new File("test-output/screenshots").toPath());
            ImageIO.write(image, "PNG", new File(absolutePath));
            return relativePath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void waitUntilThankYouPageLoads() {
        if (driver == null) return;
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("thankYouMessage")));
        } catch (Exception e) {
            System.out.println(" Could not verify Thank You page load: " + e.getMessage());
        }
    }
}
