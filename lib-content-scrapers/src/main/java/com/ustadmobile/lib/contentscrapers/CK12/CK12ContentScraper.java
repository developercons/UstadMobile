package com.ustadmobile.lib.contentscrapers.CK12;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ustadmobile.lib.contentscrapers.ContentScraper;
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil;
import com.ustadmobile.lib.contentscrapers.ScraperConstants;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.print.Doc;

public class CK12ContentScraper {

    private final String urlString;
    private final File destinationDirectory;
    private final URL scrapUrl;
    private final File assetDirectory;
    String ckK12 = "https://www.ck12.org";

    String chromeDriverLocation = "C:\\Users\\suhai\\Documents\\chromedriver_win32\\chromedriver.exe";

    String practiceTestIdLink = "https://www.ck12.org/assessment/api/get/info/test/practice/";
    String startTestLink = "https://www.ck12.org/assessment/api/start/test/";
    String questionLinkId = "https://www.ck12.org/assessment/api/render/questionInstance/test/";
    // sample questionLink 5985b3d15aa4136da1e858b8/2/5b7a41ba5aa413662008f44f

    public final String postfix = "?hints=true&evalData=true";
    public final String POLICIES = "?policies=%5B%7B%22name%22%3A%22immediate_evaluation%22%2C%22value%22%3Atrue%7D%2C%7B%22name%22%3A%22timelimit%22%2C%22value%22%3A0%7D%2C%7B%22name%22%3A%22max_questions%22%2C%22value%22%3A15%7D%2C%7B%22name%22%3A%22adaptive%22%2C%22value%22%3Atrue%7D%5D";
    public final String practicePost = "?adaptive=true";

    private ChromeDriver driver;
    private WebDriverWait waitDriver;

    public static final String READ_TYPE = "READ";
    public static final String VIDEO_TYPE = "VIDEO";
    public static final String PRACTICE_TYPE = "PRACTICE";


    public CK12ContentScraper(String url, File destDir) throws MalformedURLException {
        this.urlString = url;
        scrapUrl = new URL(url);
        this.destinationDirectory = destDir;
        assetDirectory = new File(destDir, "asset");
        assetDirectory.mkdirs();
    }

    public void scrapVideoContent() throws IOException {


        Document fullSite = setUpChromeDriver(urlString);

        Document videoContent = getContentFromSite(VIDEO_TYPE, "div.flex-video iframe");

        Elements videoElement = videoContent.select("iframe");
        String link = videoElement.attr("src");

        String imageThumnail = fullSite.select("meta[property=og:image]").attr("content");

        if (imageThumnail == null || imageThumnail.isEmpty()) {
            throw new IllegalArgumentException("Did not receive image content from meta tag");
        }

        try {
            ContentScraperUtil.downloadContent(new URL(imageThumnail), new File(assetDirectory, "video-thumbnail.jpg"));
        } catch (MalformedURLException e) {
            imageThumnail = "";
        }


        String videoSource = ContentScraperUtil.downloadAllResources(videoElement.outerHtml(), assetDirectory, scrapUrl);

        if (link == null || link.isEmpty()) {
            throw new IllegalArgumentException("Have not finished support of video type link " + link + " for url " + urlString);
        }

        String videoTitleHtml = getTitleHtml(fullSite);

        String detailHtml = removeAllHref(getDetailSectionHtml(fullSite));

        String indexHtml = videoTitleHtml + videoSource + detailHtml;

        FileUtils.writeStringToFile(new File(destinationDirectory, "index.html"), indexHtml, ScraperConstants.UTF_ENCODING);
    }

    public void scrapReadContent() throws IOException {

        Document html = setUpChromeDriver(urlString);
        //Document html = getContentFromSite(READ_TYPE, "");

        String readTitle = getTitleHtml(html);

        String readHtml = removeAllHref(ContentScraperUtil.downloadAllResources(getContentHtml(html), assetDirectory, scrapUrl));

        String detailHtml = removeAllHref(getDetailSectionHtml(html));

        // append the title
        readHtml = readTitle + readHtml + detailHtml;

        FileUtils.writeStringToFile(new File(destinationDirectory, "index.html"), readHtml, ScraperConstants.UTF_ENCODING);

    }

    public void scrapPracticeContent() throws IOException {

        String practiceUrl = urlString.substring(urlString.lastIndexOf("/") + 1, urlString.indexOf("?"));

        String testIdLink = practiceTestIdLink + practiceUrl + practicePost;

        PracticeResponse response = ContentScraperUtil.parseJson(new URL(testIdLink), PracticeResponse.class);

        String testId = response.response.test.id;
        int goal = response.response.test.goal;

        String testLink = startTestLink + testId + POLICIES;
        TestResponse testResponse = ContentScraperUtil.parseJson(new URL(testLink), TestResponse.class);

        String testScoreId = testResponse.response.testScore.id;

        Gson gson = new GsonBuilder().create();

        ArrayList<QuestionResponse> questionList = new ArrayList<>();
        for (int i = 1; i <= goal; i++) {

            String questionLink = questionLinkId + testId + "/" + i + "/" + testScoreId + postfix;

            QuestionResponse questionResponse = ContentScraperUtil.parseJson(new URL(questionLink), QuestionResponse.class);

            String questionId = questionResponse.response.questionID;

            File questionAsset = new File(destinationDirectory, questionId);
            questionAsset.mkdirs();

            questionResponse.response.stem.displayText = ContentScraperUtil.downloadAllResources(
                    questionResponse.response.stem.displayText, questionAsset, scrapUrl);


            List<String> hintsList = questionResponse.response.hints;
            for (int j = 0; j < hintsList.size(); j++) {
                hintsList.set(j, ContentScraperUtil.downloadAllResources(hintsList.get(j), questionAsset, scrapUrl));
            }
            questionResponse.response.hints = hintsList;

            String answerResponse = new Rhino().getResult(questionResponse.response.data);

            System.out.println(answerResponse);

            AnswerResponse answer = gson.fromJson(answerResponse, AnswerResponse.class);
            answer.instance.solution = ContentScraperUtil.downloadAllResources(answer.instance.solution, questionAsset, scrapUrl);

            answer.instance.answer = downloadAllResourcesFromAnswer(answer.instance.answer, questionAsset, scrapUrl);

            if (ScraperConstants.QUESTION_TYPE.MULTI_CHOICE.getType().equalsIgnoreCase(questionResponse.response.questionType)) {

                List<QuestionResponse.Response.QuestionObjects> questionOrderList = questionResponse.response.responseObjects;
                List<AnswerResponse.Instance.AnswerObjects> answerObjectsList = answer.instance.responseObjects;
                for (int order = 0; order < questionOrderList.size(); order++) {

                    QuestionResponse.Response.QuestionObjects question = questionOrderList.get(order);

                    question.displayText = ContentScraperUtil.downloadAllResources(question.displayText, questionAsset, scrapUrl);
                    question.optionKey = ContentScraperUtil.downloadAllResources(question.optionKey, questionAsset, scrapUrl);

                    AnswerResponse.Instance.AnswerObjects answerObject = answerObjectsList.get(order);
                    answerObject.displayText = ContentScraperUtil.downloadAllResources(answerObject.displayText, questionAsset, scrapUrl);
                    answerObject.optionKey = ContentScraperUtil.downloadAllResources(answerObject.optionKey, questionAsset, scrapUrl);

                }
            }


            questionResponse.response.answer = answer;

            questionList.add(questionResponse);

        }

        ContentScraperUtil.saveListAsJson(destinationDirectory, questionList, ScraperConstants.QUESTIONS_JSON);

    }

    private List<Object> downloadAllResourcesFromAnswer(List<Object> answer, File questionAsset, URL scrapUrl) {

        for(int i = 0; i < answer.size(); i++) {

            Object object = answer.get(i);
            if(object instanceof String){
                answer.set(i, ContentScraperUtil.downloadAllResources((String) object, questionAsset, scrapUrl));
            }else if(object instanceof List<?>){
                answer.set(i, downloadAllResourcesFromAnswer((List<Object>) object, questionAsset, scrapUrl));
            }
        }

        return answer;
    }

    private String getTitleHtml(Document section) {

        return section.select("div.title").outerHtml();
    }

    private String getDetailSectionHtml(Document section) {

        return section.select("div.metadataview").html();

    }

    private String getContentHtml(Document section) {
        return section.select("div.modalitycontent").html();
    }

    private String removeAllHref(String html) {

        Document doc = Jsoup.parse(html);

        Elements elements = doc.select("[href]");

        for (Element element : elements) {
            element.removeAttr("href");
        }

        return doc.body().html();
    }

 /*   private String getResourceSectionHtml(Document section) throws IOException {

        String path = section.select("section.resources_container").attr("data-loadurl");

        Document document = Jsoup.connect(new URL(scrapUrl, path).toString()).get();

        Elements resources = document.select("div.resource_row a.break-word");

        StringBuilder htmlString = new StringBuilder();
        for (Element resourceLink : resources) {

            htmlString.append(resourceLink.outerHtml());

        }
        return htmlString.toString();
    } */

    private Document getContentFromSite(String type, String waitForText) {

        WebElement element = waitDriver.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(waitForText)));

        if (type.equalsIgnoreCase(VIDEO_TYPE)) {
            return Jsoup.parse(driver.switchTo().frame(element).getPageSource());
        }

        if (type.equalsIgnoreCase(READ_TYPE)) {
            return Jsoup.parse(driver.getPageSource());
        }

        return null;
    }


    /**
     * Given a url, Setup chrome web driver and wait for page to be rendered
     *
     * @param url
     */
    public Document setUpChromeDriver(String url) {
        System.setProperty("webdriver.chrome.driver", chromeDriverLocation);
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setHeadless(true);
        driver = new ChromeDriver(chromeOptions);

        driver.get(url);

        waitDriver = new WebDriverWait(driver, 30);
        waitForJSandJQueryToLoad();

        return Jsoup.parse(driver.getPageSource());
    }

    private boolean waitForJSandJQueryToLoad() {

        // wait for jQuery to load
        ExpectedCondition<Boolean> jQueryLoad = driver -> {
            try {
                return ((Long) ((JavascriptExecutor) driver).executeScript("return jQuery.active") == 0);
            } catch (Exception e) {
                // no jQuery present
                return true;
            }
        };

        // wait for Javascript to load
        ExpectedCondition<Boolean> jsLoad = driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState")
                .toString().equals("complete");

        return waitDriver.until(jQueryLoad) && waitDriver.until(jsLoad);
    }


}
