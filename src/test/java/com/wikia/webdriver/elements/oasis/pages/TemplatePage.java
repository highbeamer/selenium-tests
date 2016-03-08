package com.wikia.webdriver.elements.oasis.pages;

import lombok.Getter;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.wikia.webdriver.common.contentpatterns.URLsContent;
import com.wikia.webdriver.elements.oasis.components.templateclassificiation.TemplateClassification;
import com.wikia.webdriver.pageobjectsfactory.pageobject.PortableInfobox;
import com.wikia.webdriver.pageobjectsfactory.pageobject.WikiBasePageObject;
import com.wikia.webdriver.pageobjectsfactory.pageobject.article.editmode.SourceEditModePageObject;

public class TemplatePage extends WikiBasePageObject {

  @Getter(lazy = true)
  private final TemplateClassification templateClassification = new TemplateClassification();
  private final PortableInfobox portableInfobox = new PortableInfobox();

  @FindBy(css = "#ca-edit")
  protected WebElement editUsingClassicEditor;

  public TemplatePage() {
    super();
  }

  public TemplatePage open(String templateName) {
    getUrl(String.format("%s%s%s:%s", urlBuilder.getUrlForWiki(), URLsContent.WIKI_DIR,
        URLsContent.TEMPLATE_NAMESPACE, templateName));

    return this;
  }

  public SourceEditModePageObject editArticleInSrcUsingDropdown() {
    editUsingClassicEditor.click();
    return new SourceEditModePageObject(driver);
  }

  public TemplatePage openArticleByName(String wikiURL, String articleName) {
    getUrl(wikiURL + URLsContent.WIKI_DIR + articleName);
    return new TemplatePage();
  }

  public PortableInfobox getPortableInfobox() {
    return portableInfobox;
  }
}
