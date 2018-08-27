package com.ustadmobile.lib.contentscrapers.EdraakK12;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil;
import com.ustadmobile.lib.contentscrapers.ScraperConstants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static com.ustadmobile.lib.contentscrapers.ScraperConstants.*;


public class EdraakK12ContentScraper {

    private final String url;
    private final File destinationDirectory;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: <edraak k12 json url> <file destination>");
            System.exit(1);
        }

        System.out.println(args[0]);
        System.out.println(args[1]);
        try {
            new EdraakK12ContentScraper(args[0], new File(args[1])).scrapContent();
        } catch (IOException e) {
            System.err.println("Exception running scrapContent");
            e.printStackTrace();
        }

    }

    public EdraakK12ContentScraper(String url, File destinationDir) {
        this.url = url;
        this.destinationDirectory = destinationDir;
    }

    public static String generateUrl(String baseUrl, String contentId, int programId) {
        System.out.println("scrapContent url = " + baseUrl + "component/" + contentId + "/?states_program_id=" + programId);

        return baseUrl + "component/" + contentId + "/?states_program_id=" + programId;
    }

    /**
     * Given a url and a directory, download all its content and save it in a directory
     *
     * @throws IOException
     */
    public void scrapContent() throws IOException {

        URL scrapUrl;
        try {
            scrapUrl = new URL(url);
        } catch (MalformedURLException e) {
            System.out.println("Scrap Malformed url" + url);
            throw new IllegalArgumentException("Malformed url" + url, e);
        }

        destinationDirectory.mkdirs();

        ContentResponse response;
        try {
            response = ContentScraperUtil.parseJson(scrapUrl, ContentResponse.class);
        } catch (IOException | JsonSyntaxException e) {
            throw new IllegalArgumentException("JSON INVALID", e.getCause());
        }

        if (!ContentScraperUtil.isImportedComponent(response.component_type))
            throw new IllegalArgumentException("Not an imported content type!");

        if (response.target_component == null || response.target_component.children == null)
            throw new IllegalArgumentException("Null target component, or target component children are null");

        boolean anyContentUpdated;

        List<ContentResponse> questionsList = getQuestionSet(response);
        try {
            anyContentUpdated = findAllExerciseImages(questionsList, destinationDirectory, scrapUrl);
        } catch (IOException e) {
            throw new IllegalArgumentException("Exercise Malformed", e.getCause());
        }

        if (ComponentType.ONLINE.getType().equalsIgnoreCase(response.target_component.component_type)) {

            // Contains children which have video and question set list
            for (ContentResponse children : response.target_component.children) {

                if (ScraperConstants.ComponentType.VIDEO.getType().equalsIgnoreCase(children.component_type)) {

                    if (children.video_info == null || children.video_info.encoded_videos == null || children.video_info.encoded_videos.isEmpty())
                        throw new IllegalArgumentException("Component Type was Video but no video found");

                    ContentResponse.Encoded_videos videoHref = selectVideo(children.video_info.encoded_videos);
                    URL videoUrl;
                    try {
                        videoUrl = new URL(scrapUrl, videoHref.url);
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("video Malformed url", e);
                    }


                    File videoFile = new File(destinationDirectory, VIDEO_MP4);
                    if (ContentScraperUtil.isContentUpdated(ContentScraperUtil.parseEdraakK12Date(videoHref.modified), videoFile)) {
                        try {
                            ContentScraperUtil.downloadContent(videoUrl, videoFile);
                            anyContentUpdated = true;
                        } catch (IOException e) {
                            throw new IllegalArgumentException("Download Video Malformed url", e);
                        }
                    }

                }

            }
        }

        File contentJsonFile = new File(destinationDirectory, ScraperConstants.CONTENT_JSON);
        if (anyContentUpdated || !ContentScraperUtil.fileHasContent(contentJsonFile)) {
            // store the json in a file after modifying image links
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String jsonString = gson.toJson(response);
            FileUtils.writeStringToFile(contentJsonFile, jsonString, ScraperConstants.UTF_ENCODING);
            anyContentUpdated = true;
        }

        File tinCanFile = new File(destinationDirectory, "tincan.xml");
        if (!ContentScraperUtil.fileHasContent(tinCanFile)) {
            try {
                ContentScraperUtil.generateTinCanXMLFile(destinationDirectory, response.title, "ar",
                        ScraperConstants.INDEX_HTML, "http://adlnet.gov/expapi/activities/module",
                        url.substring(0, url.indexOf("component/")) + response.id,
                        "", "en");
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
            anyContentUpdated = true;
        }

        // add these files into the directory
        anyContentUpdated = writeFileToDirectory(ScraperConstants.JS_HTML_TAG, new File(destinationDirectory, INDEX_HTML)) || anyContentUpdated;
        anyContentUpdated = writeFileToDirectory(ScraperConstants.JS_TAG, new File(destinationDirectory, JQUERY_JS)) || anyContentUpdated;
        anyContentUpdated = writeFileToDirectory(ScraperConstants.MATERIAL_CSS_LINK, new File(destinationDirectory, MATERIAL_CSS)) || anyContentUpdated;
        anyContentUpdated = writeFileToDirectory(ScraperConstants.MATERIAL_JS_LINK, new File(destinationDirectory, ScraperConstants.MATERIAL_JS)) || anyContentUpdated;
        anyContentUpdated = writeFileToDirectory(ScraperConstants.REGULAR_ARABIC_FONT_LINK, new File(destinationDirectory, ScraperConstants.ARABIC_FONT_REGULAR)) || anyContentUpdated;
        anyContentUpdated = writeFileToDirectory(ScraperConstants.BOLD_ARABIC_FONT_LINK, new File(destinationDirectory, ScraperConstants.ARABIC_FONT_BOLD)) || anyContentUpdated;


        // nothing changed, keep same files
        if (anyContentUpdated) {
            ContentScraperUtil.zipDirectory(destinationDirectory, response.id, destinationDirectory.getParentFile());
        }

    }

    /**
     * Given an asset location, write into a file and return true if it was created. return false if it was created before
     *
     * @param input
     * @param file
     */
    public boolean writeFileToDirectory(String input, File file) {

        if (ContentScraperUtil.fileHasContent(file)) {
            return false;
        }

        InputStream htmlIns = getClass().getResourceAsStream(input);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = htmlIns.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            UMIOUtils.closeQuietly(htmlIns);
            UMIOUtils.closeQuietly(outputStream);
        }

        return true;
    }


    /**
     * Find and return question set for imported content
     *
     * @param response
     * @return
     */
    public List<ContentResponse> getQuestionSet(ContentResponse response) {

        if (ComponentType.ONLINE.getType().equalsIgnoreCase(response.target_component.component_type)) {

            for (ContentResponse children : response.target_component.children) {
                if (ScraperConstants.QUESTION_SET_HOLDER_TYPES.contains(children.component_type)) {

                    return children.question_set.children;
                }
            }
        } else if (ComponentType.TEST.getType().equalsIgnoreCase(response.target_component.component_type)) {

            return response.target_component.question_set.children;

        }
        return null;
    }


    /**
     * Given an array of questions, find the questions that have image tags in their html and save the image within the directory
     * Finally write the list into a file
     *
     * @param questionsList
     * @param destinationDir
     * @throws IOException
     */
    private boolean findAllExerciseImages(List<ContentResponse> questionsList, File destinationDir, URL url) throws IOException {

        if (questionsList == null || questionsList.isEmpty())
            throw new IllegalArgumentException("No Questions were found in the question set");

        int exerciseUpdatedCount = 0;
        for (ContentResponse exercise : questionsList) {

            File exerciseDirectory = new File(destinationDir, exercise.id);
            if (ContentScraperUtil.isContentUpdated(ContentScraperUtil.parseEdraakK12Date(exercise.updated), exerciseDirectory)) {

                exerciseDirectory.mkdirs();
                exerciseUpdatedCount++;

                exercise.full_description = ContentScraperUtil.downloadAllResources(exercise.full_description, exerciseDirectory, url);
                exercise.explanation = ContentScraperUtil.downloadAllResources(exercise.explanation, exerciseDirectory, url);
                exercise.description = ContentScraperUtil.downloadAllResources(exercise.description, exerciseDirectory, url);

                if (ComponentType.MULTICHOICE.getType().equalsIgnoreCase(exercise.component_type)) {
                    for (ContentResponse.Choice choice : exercise.choices) {
                        choice.description = ContentScraperUtil.downloadAllResources(choice.description, exerciseDirectory, url);
                    }
                }

                for (ContentResponse.Hint hint : exercise.hints) {
                    hint.description = ContentScraperUtil.downloadAllResources(hint.description, exerciseDirectory, url);
                }
            }

        }

        try {
            if (exerciseUpdatedCount > 0) {
                ContentScraperUtil.saveListAsJson(destinationDir, questionsList, QUESTIONS_JSON);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid Questions Json");
        }

        return exerciseUpdatedCount > 0;
    }


    /**
     * Given a list of videos, find the one with the smallest size
     *
     * @param encoded_videos
     * @return chosen video url
     */
    private ContentResponse.Encoded_videos selectVideo(List<ContentResponse.Encoded_videos> encoded_videos) {

        ContentResponse.Encoded_videos selectedVideo = null;
        int videoSize = Integer.MAX_VALUE;

        for (ContentResponse.Encoded_videos videos : encoded_videos) {
            if (videos.file_size > 0 && videos.file_size < videoSize) {
                selectedVideo = videos;
            }
        }
        return selectedVideo;
    }


}
