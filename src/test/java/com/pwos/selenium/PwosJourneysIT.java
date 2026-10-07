package com.pwos.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PwosJourneysIT extends SeleniumBase {

    private String uniq() {
        return String.valueOf(System.currentTimeMillis());
    }

    private By row(String name) {
        return By.xpath(
            "//tr[td[normalize-space()='" + name + "']]");
    }

    private By submitButton() {
        return By.cssSelector("form.form-row button");
    }

    private void addItem(String name, String sku,
                         int qty, int threshold) {
        driver.get(BASE_URL + "/items");
        driver.findElement(By.name("name")).sendKeys(name);
        driver.findElement(By.name("sku")).sendKeys(sku);
        driver.findElement(By.name("quantity"))
              .sendKeys(String.valueOf(qty));
        driver.findElement(By.name("reorderThreshold"))
              .sendKeys(String.valueOf(threshold));
        driver.findElement(submitButton()).click();
        wait.until(ExpectedConditions
              .presenceOfElementLocated(row(name)));
    }

    private void createOrder(String itemName, int qty,
                             String dueDate) {
        driver.get(BASE_URL + "/orders");
        Select item = new Select(
            driver.findElement(By.name("itemId")));
        String label = item.getOptions().stream()
            .map(WebElement::getText)
            .filter(t -> t.contains(itemName))
            .findFirst()
            .orElseThrow();
        item.selectByVisibleText(label);
        driver.findElement(By.name("quantity"))
              .sendKeys(String.valueOf(qty));
        if (dueDate != null) {
            WebElement date =
                driver.findElement(By.name("dueDate"));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];",
                date, dueDate);
        }
        driver.findElement(submitButton()).click();
        wait.until(ExpectedConditions
              .presenceOfElementLocated(row(itemName)));
    }

    @Test
    void j1_addSearchAndEditItem() {
        String name = "SelItem-" + uniq();
        String sku = "SEL-" + uniq();
        addItem(name, sku, 100, 10);
        String shown = driver.findElement(row(name))
            .findElement(By.xpath("./td[3]")).getText();
        assertEquals(sku, shown, "SKU in catalogue row");

        driver.get(BASE_URL + "/items?q=" + name);
        assertEquals(1, driver.findElements(By.xpath("//tr[td]")).size(),
            "Search should return exactly one item");

        driver.findElement(row(name))
              .findElement(By.cssSelector("a.btn-edit")).click();
        WebElement qty = wait.until(ExpectedConditions
              .visibilityOfElementLocated(By.name("quantity")));
        qty.clear();
        qty.sendKeys("42");
        driver.findElement(submitButton()).click();
        wait.until(ExpectedConditions
              .presenceOfElementLocated(row(name)));
        assertEquals("42", driver.findElement(row(name))
              .findElement(By.xpath("./td[4]")).getText(),
              "Quantity after edit");
    }

    @Test
    void j2_lowStockItemShowsAlert() {
        String name = "SelLow-" + uniq();
        addItem(name, "SEL-" + uniq(), 3, 5);
        WebElement alert = driver.findElement(By.xpath(
            "//div[contains(@class,'alert')]"
            + "[contains(.,'Low Stock Alert')]"));
        assertTrue(alert.getText().contains(name),
            "Low-stock alert should list " + name);
    }

    @Test
    void j3_createEditAndSearchWorkOrder() {
        String name = "SelOrder-" + uniq();
        addItem(name, "SEL-" + uniq(), 100, 10);
        createOrder(name, 5, null);
        WebElement r = driver.findElement(row(name));
        assertEquals("5",
            r.findElement(By.xpath("./td[3]")).getText(),
            "Order quantity");
        assertEquals("PENDING",
            r.findElement(By.cssSelector("span.badge"))
             .getText(), "Initial status");

        r.findElement(By.cssSelector("a.btn-edit")).click();
        WebElement qty = wait.until(ExpectedConditions
              .visibilityOfElementLocated(By.name("quantity")));
        qty.clear();
        qty.sendKeys("7");
        driver.findElement(submitButton()).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(row(name)));
        assertEquals("7", driver.findElement(row(name))
              .findElement(By.xpath("./td[3]")).getText(),
              "Order quantity after edit");

        driver.get(BASE_URL + "/orders?status=PENDING&q=" + name);
        assertEquals(1, driver.findElements(By.xpath("//tr[td]")).size(),
            "Pending + name search should return exactly one order");

        driver.get(BASE_URL + "/orders?status=DONE&q=" + name);
        assertEquals(0, driver.findElements(By.xpath("//tr[td]")).size(),
            "Done + name search should return no orders");
    }

    @Test
    void j4_doneOrderAddsStock() {
        String name = "SelStatus-" + uniq();
        addItem(name, "SEL-" + uniq(), 100, 10);
        createOrder(name, 5, null);
        By badge = By.xpath("//tr[td[normalize-space()='" + name
            + "']]//span[contains(@class,'badge')]");
        By statusSelect = By.xpath("//tr[td[normalize-space()='" + name
            + "']]//select[@name='status']");

        new Select(driver.findElement(statusSelect)).selectByValue("IN_PROGRESS");
        wait.until(ExpectedConditions.textToBe(badge, "IN_PROGRESS"));

        new Select(driver.findElement(statusSelect)).selectByValue("DONE");
        wait.until(ExpectedConditions.textToBe(badge, "DONE"));

        driver.get(BASE_URL + "/items?q=" + name);
        assertEquals("105", driver.findElement(row(name))
              .findElement(By.xpath("./td[4]")).getText(),
              "Stock after the order is done (100 + 5)");
    }

    @Test
    void j5_overdueOrderAlertAndCancel() {
        String name = "SelOverdue-" + uniq();
        addItem(name, "SEL-" + uniq(), 100, 10);
        String yesterday = LocalDate.now().minusDays(1).toString();
        createOrder(name, 5, yesterday);
        WebElement alert = driver.findElement(By.xpath(
            "//div[contains(@class,'alert')][contains(.,'Overdue Orders')]"));
        assertTrue(alert.isDisplayed(), "Overdue alert visible");
        assertEquals(yesterday, driver.findElement(row(name))
              .findElement(By.xpath("./td[4]")).getText(),
              "Due date in the order row");

        driver.findElement(row(name))
              .findElement(By.cssSelector("button.btn-cancel")).click();
        By badge = By.xpath("//tr[td[normalize-space()='" + name
            + "']]//span[contains(@class,'badge')]");
        wait.until(ExpectedConditions.textToBe(badge, "CANCELLED"));
    }

}
