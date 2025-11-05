package org.test;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestListener implements ITestListener {

    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();
    public static WebDriver driver;

    @Override
    public void onStart(ITestContext context) {
        ExtentSparkReporter reporter = new ExtentSparkReporter("test-output/ExtentReport.html");
        extent = new ExtentReports();
        extent.attachReporter(reporter);

        // Add system info to report
        extent.setSystemInfo("Tester", "QA Team");
        extent.setSystemInfo("Environment", "Production");
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
    }

    @Override
    public void onTestStart(ITestResult result) {
        test.set(extent.createTest(result.getName()));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String screenshotPath = captureScreenshot(result.getName());
        if (screenshotPath != null) {
            test.get().pass("Test Passed").addScreenCaptureFromPath(screenshotPath);
        } else {
            test.get().pass("Test Passed - No screenshot available");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String screenshotPath = captureScreenshot(result.getName());
        if (screenshotPath != null) {
            test.get().fail(result.getThrowable()).addScreenCaptureFromPath(screenshotPath);
        } else {
            test.get().fail(result.getThrowable());
        }
    }

    private String captureScreenshot(String testName) {
        if (driver == null) return null;
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String relativePath = "screenshots/" + testName + "_" + date + ".png";  // relative path for report
        String absolutePath = "test-output/" + relativePath;
        try {
            Files.createDirectories(new File("test-output/screenshots").toPath());
            Files.copy(srcFile.toPath(), new File(absolutePath).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return relativePath;
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
        }
    }
}
