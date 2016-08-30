/*
    This file is part of Ustad Mobile.

    Ustad Mobile Copyright (C) 2011-2014 UstadMobile Inc.

    Ustad Mobile is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
    LLC must be kept as they are in the original distribution.  If any new
    screens are added you must include the Ustad Mobile logo as it has been
    used in the original distribution.  You may not create any new
    functionality whose purpose is to diminish or remove the Ustad Mobile
    Logo.  You must leave the Ustad Mobile logo as the logo for the
    application to be used with any launcher (e.g. the mobile app launcher).

    If you want a commercial license to remove the above restriction you must
    contact us.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Ustad Mobile is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */
package com.ustadmobile.port.j2me.view.idevice;

import com.sun.lwuit.html.HTMLCallback;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.html.HTMLElement;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.core.util.UMTinCanUtil;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 *
 * This class represents a Quiz Question that was made in eXeLearning and
 * handles hiding and showing the feedback for each answer
 * 
 * @author mike
 */
public class EXEQuizQuestion {
    
    private String id;
    
    private Vector answers;
        
    private HTMLElement formEl;
    
    private HTMLElement questionEl;
    
    protected int questionNum;
    
    //The prefix to be found on the form elements generated by eXeLearning
    static final String PREFIX_FORMNAME = "multi-choice-form-";
    
    private int[] QUESTION_TAG_IDS = new int[] {HTMLElement.TAG_DIV, 
        HTMLElement.TAG_SECTION};
    
    private EXEQuizAnswer currentSelectedAnswer;
    
    EXEQuizIdevice iDevice;

    /**
     * Make a new object representing an eXeLearning quiz question
     * 
     * @param qFormEl HTMLElement representing the form eXeLearning made for this question
     * @param questionNum The index of this question out of all questions in the idevice
     * @param iDevice the EXEQuizIdevice this question is part of
     */
    public EXEQuizQuestion(HTMLElement qFormEl, EXEQuizIdevice iDevice, int questionNum) {
        this.iDevice = iDevice;
        this.questionNum = questionNum;
        setupFromElement(qFormEl);
        currentSelectedAnswer = null;
    }
    
    private void setupFromElement(HTMLElement qFormEl) {
        String formName = qFormEl.getAttributeById(HTMLElement.ATTR_NAME);
        id = formName.substring(PREFIX_FORMNAME.length());
        formEl = qFormEl;
        questionEl = (HTMLElement)qFormEl.getElementById("taquestion" + id);
        answers = new Vector();
        
        //find the potential answers for this question
        Vector answerEls = qFormEl.getDescendantsByClass("iDevice_answer", 
            QUESTION_TAG_IDS);
        
        for(int i = 0; i < answerEls.size(); i++) {
            HTMLElement answerEl = (HTMLElement)answerEls.elementAt(i);
            EXEQuizAnswer answer = new EXEQuizAnswer(i, this, answerEl, qFormEl);
            answers.addElement(answer);
        }
    }
    
    public JSONObject getTinCanObject() {
        return UMTinCanUtil.makeActivityObjectById(UMFileUtil.joinPaths(new String[] {
            iDevice.getPageTinCanId(), getID()}));
    }
    
    /**
     * Get the HTMLElement that contains the question itself
     * 
     * @return Element containing the question itself
     */
    public HTMLElement getQuestionElement() {
        return questionEl;
    }
    
    HTMLElement getFormElement() {
        return formEl;
    }
    
    String getID() {
        return id;
    }
    
    public Vector getAnswers() {
        return answers;
    }
    
    
    /**
     * Add a possible answer for this question
     * 
     * @param answer 
     */
    public void addAnswer(EXEQuizAnswer answer) {
        answers.addElement(answer);
    }
    
    /**
     * Find the answer represented by an input element (eg. the radio button)
     * 
     * @param inputEl The input Element from the document
     * @return The answer elment that this input element represents or null if none
     */
    public EXEQuizAnswer getAnswerByInputElement(HTMLElement inputEl) {
        EXEQuizAnswer curAnswer;
        for(int i = 0; i < answers.size(); i++) {
            curAnswer = (EXEQuizAnswer)answers.elementAt(i);
            if(curAnswer.getInputElement() == inputEl) {
                return curAnswer;
            }
        }
        
        return null;
    }
    
    /**
     * Find the answer by the ID of the answer itself
     * 
     * @param id ID of the answer to find
     * @return The EXEQuizAnswer object if found, null otherwise
     */
    public EXEQuizAnswer getAnswerById(String id) {
        EXEQuizAnswer curAnswer;
        for(int i = 0; i < answers.size(); i++) {
            curAnswer = (EXEQuizAnswer)answers.elementAt(i);
            if(curAnswer.getID().equals(id)) {
                return curAnswer;
            }
        }
        
        return null;
    }
    
    /**
     * Handles when the user has selected an answer (e.g. the selected the 
     * radio button)
     * 
     * @param inputElement Input Element that was acted upon
     * @return true if changes have been made to the DOM, false otherwise
     */
    public boolean handleSelectAnswer(HTMLElement inputElement, HTMLComponent htmlC) {
        EXEQuizAnswer selectedAnswer = getAnswerByInputElement(inputElement);
        JSONObject iState = iDevice.getState();
        if(iDevice.getState() != null) {
            selectedAnswer.setStateSelectedAnswer(iDevice.getState());
        }
        
        boolean domChanged = showMCQFeedback(selectedAnswer, htmlC);
        UstadMobileSystemImpl.getInstance().queueTinCanStatement(
            selectedAnswer.makeTinCanStmt(), iDevice.getContext());
        return domChanged;
    }
    
    /**
     * Set the state of this question - this should be a JSON object in the form
     * of : {'response': 'answerid', 'score' : xx.yy} - the same as the JSON
     * which is stored in localStorage in the HTML5 version.
     * 
     * @param state State object for this question as described above.
     */
    public void setState(JSONObject state) throws JSONException {
        String answerId = state.optString("response", null);
        if(answerId != null) {
            EXEQuizAnswer answer = getAnswerById(answerId);
            if(answer != null) {
                answer.getInputElement().setAttribute("checked", "checked");
                showMCQFeedback(answer, false, null);
            }
        }
    }
    
    /**
     * Show the MCQ feedback - show the fedback for the selected answer and hide
     * all others
     * 
     * @param selectedAnswer 
     * @param scrollToFeedback If true scroll to the feedback element
     */
    public boolean showMCQFeedback(EXEQuizAnswer selectedAnswer, boolean scrollToFeedback, HTMLComponent htmlC) {
        if(currentSelectedAnswer == selectedAnswer) {
            //do nothing
            return false;
        }
        
        EXEQuizAnswer curAnswer;
        for(int i = 0; i < answers.size(); i++) {
            curAnswer = (EXEQuizAnswer)answers.elementAt(i);
            if(curAnswer == selectedAnswer) {
                curAnswer.showFeedback(htmlC);
            }else {
                curAnswer.hideFeedback();
            }
        }
        currentSelectedAnswer = selectedAnswer;
        HTMLElement feedbackEl = selectedAnswer.getFeedbackElement();
        
        if(scrollToFeedback && htmlC != null) {
            try {
                htmlC.scrollToElement(feedbackEl, true);
            }catch(Exception e) {
                //can happen if HTML Component doesn't feel fully loaded
                UstadMobileSystemImpl.l(UMLog.WARN, 202, this.id);
            }
        }
        
        return true;
    }
    
    public boolean showMCQFeedback(EXEQuizAnswer selectedAnswer, HTMLComponent htmlC) {
        return showMCQFeedback(selectedAnswer, true, htmlC);
    }
    
    /**
     * The default arrangement as generated by eXeLearning depends on CSS to place
     * the radio buttons next to the answers they represent : that doesn't work
     * so well with the HTMLComponent.  This method will change this into a
     * table instead.
     * 
     * @return 
     */
    public boolean formatQuestionAsTable() {
        if(answers.size() == 0) {
            return false;
        }
        
        HTMLElement answerContainer = (HTMLElement)formEl.getDescendantsByClass(
            "iDevice_answers", QUESTION_TAG_IDS).elementAt(0);
        HTMLElement tableEl = new HTMLElement("table");
        HTMLElement tr;
        HTMLElement radioTd;
        HTMLElement answerTd;
        HTMLElement inputParent;
        HTMLElement answerContentParent;
        EXEQuizAnswer currentAnswer;
        for(int i = 0; i < answers.size(); i++) {
            currentAnswer = (EXEQuizAnswer)answers.elementAt(i);
            tr = new HTMLElement("tr");
            
            radioTd = new HTMLElement("td");
            inputParent = (HTMLElement)currentAnswer.getInputElement().getParent();
            inputParent.removeChildAt(inputParent.getChildIndex(
                    currentAnswer.getInputElement()));
            radioTd.addChild(currentAnswer.getInputElement());
            
            answerTd = new HTMLElement("td");
            answerContentParent = 
                (HTMLElement)currentAnswer.getAnswerContentElement().getParent();
            answerContentParent.removeChildAt(
                answerContentParent.getChildIndex(
                currentAnswer.getAnswerContentElement()));
            answerTd.addChild(currentAnswer.getAnswerContentElement());
            
            tr.addChild(radioTd);
            tr.addChild(answerTd);
            tableEl.addChild(tr);
        }
        
        answerContainer.insertChildAt(tableEl, 0);
        return true;
    }
    
    
}
