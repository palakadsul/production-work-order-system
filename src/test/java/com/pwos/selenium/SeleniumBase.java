package com.pwos.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@ExtendWith(ScreenshotOnFailure.class)
public abstract class SeleniumBase {

    protected static final String BASE_URL =
            System.getProperty("base.url", "http://localhost:8080");
    protected static final String USER =
            System.getProperty("pwos.user", "admin");
    protected static final String PASSWORD =
            System.getProperty("pwos.password", "admin123");

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1400,1000");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        login(USER, PASSWORD);
    }

    /** Signs in through the login page and waits for the user panel. */
    protected void login(String username, String password) {
        driver.get(BASE_URL + "/login");
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.cssSelector("form.login-form button")).click();
        wait.until(ExpectedConditions
                .presenceOfElementLocated(By.cssSelector(".user-name")));
    }

    /** Signs out with the sidebar button and waits for the login page. */
    protected void logout() {
        driver.findElement(By.cssSelector("button.btn-logout")).click();
        wait.until(ExpectedConditions
                .presenceOfElementLocated(By.name("username")));
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    public WebDriver getDriver() {
        return driver;
    }
}
