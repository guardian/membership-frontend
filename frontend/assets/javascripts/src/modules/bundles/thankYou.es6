import * as cookie from 'src/utils/cookie';
import { getQueryParameterByName, getPath } from 'src/utils/url';
import bean from 'bean'

'use strict';


const THANK_YOU_CTA_SELECTOR = '.bundle-thankyou__email-form__submit-button';
const EMAIL_FIELD_SELECTOR = '.bundle-thankyou__email-form__email'

const THANK_YOU_CTA = document.querySelectorAll(THANK_YOU_CTA_SELECTOR);
const EMAIL_FIELD = document.querySelectorAll(EMAIL_FIELD_SELECTOR);

const LIST_ID = 3818;

export function init() {
    if (THANK_YOU_CTA.length > 0) {
        bindSubmitButton()
        return;
    }
}

function bindSubmitButton() {
    bean.on(THANK_YOU_CTA[0], 'click', function(e) {
        let email = EMAIL_FIELD[0].value;
        submitForm(email, LIST_ID).then(showThankYouMessage)
    });
}


function submitForm(email, listID) {
    var formQueryString =
        'email=' + email + '&' +
        'listId=' + listID;

    return fetch(
        'https://www.theguardian.com/email',
        {
            method: 'post',
            body: formQueryString,
            headers: {
                'Accept': 'application/json'
            }
        });
}

function showThankYouMessage(){
    console.log("THANK YOU FOR YOUR EMAIL =)");
}







//  1. Enter sheet name where data is to be written below
const SHEET_NAME = "Sheet1";



var SCRIPT_PROP = PropertiesService.getScriptProperties(); // new property service

// If you don't want to expose either GET or POST methods you can comment out the appropriate function
function doGet(e){
    return handleResponse(e);
}

function doPost(e){
    return handleResponse(e);
}

function handleResponse(e) {
    // shortly after my original solution Google announced the LockService[1]
    // this prevents concurrent access overwritting data
    // [1] http://googleappsdeveloper.blogspot.co.uk/2011/10/concurrency-and-google-apps-script.html
    // we want a public lock, one that locks for all invocations
    var lock = LockService.getPublicLock();
    lock.waitLock(30000);  // wait 30 seconds before conceding defeat.

    try {
        // next set where we write the data - you could write to multiple/alternate destinations
        var doc = SpreadsheetApp.openById(SCRIPT_PROP.getProperty("key"));
        var sheet = doc.getSheetByName(SHEET_NAME);

        // we'll assume header is in row 1 but you can override with header_row in GET/POST data
        var headRow = e.parameter.header_row || 1;
        var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
        var nextRow = sheet.getLastRow()+1; // get next row
        var row = [];
        // loop through the header columns
        for (i in headers){
            if (headers[i] == "Timestamp"){ // special case if you include a 'Timestamp' column
                row.push(new Date());
            } else { // else use header name to get data
                row.push(e.parameter[headers[i]]);
            }
        }
        // more efficient to set values as [][] array than individually
        sheet.getRange(nextRow, 1, 1, row.length).setValues([row]);
        // return json success results
        return ContentService
            .createTextOutput(JSON.stringify({"result":"success", "row": nextRow}))
            .setMimeType(ContentService.MimeType.JSON);
    } catch(e){
        // if error return this
        return ContentService
            .createTextOutput(JSON.stringify({"result":"error", "error": e}))
            .setMimeType(ContentService.MimeType.JSON);
    } finally { //release lock
        lock.releaseLock();
    }
}

function setup() {
    var doc = SpreadsheetApp.getActiveSpreadsheet();
    SCRIPT_PROP.setProperty("key", doc.getId());
}
