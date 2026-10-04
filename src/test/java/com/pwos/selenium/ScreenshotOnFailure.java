package com.pwos.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotOnFailure implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
            throws Throwable {
        Object instance = context.getRequiredTestInstance();
        if (instance instanceof SeleniumBase base
                && base.getDriver() instanceof TakesScreenshot shooter) {
            try {
                Path dir = Paths.get("target", "screenshots");
                Files.createDirectories(dir);
                String stamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                String file = context.getRequiredTestClass().getSimpleName() + "_"
                        + context.getRequiredTestMethod().getName() + "_" + stamp + ".png";
                Files.copy(shooter.getScreenshotAs(OutputType.FILE).toPath(), dir.resolve(file));
                System.out.println("Failure screenshot saved: target/screenshots/" + file);
            } catch (Exception e) {
                System.err.println("Could not save screenshot: " + e.getMessage());
            }
        }
        throw throwable;
    }
}
