;
import org.json.simple.*
import org.junit.Test
import org.xwiki.hpqc.api.HPQCMacro

class TestHPQCMacro  {


    @Test
    public void test() {

	/**
	 * Credentials.
	 */
	def HPQC_PROJECT_USER = ""
	def HPQC_PROJECT_PASS = ""


	/**
	 * HPQC Information.
	 */
	def HPQC_ISSUE_CUSTOM = "";
	def HPQC_HOST_URL = "http://localhost:8080";
	def HPQC_DOMAIN_NAME = "";
	def HPQC_PROJECT_NAME = "";
	def closedStatusValue = "Closed";


	/**
	 *Fetch the information from Xwiki.
	 */
	def wikiname = doc.getDocument().getWikiName()
	def customObject =
		xwiki.getDocument("$wikiname:HPQCMacro.HPQCMacroConfiguration").getObject("$wikiname:HPQCMacro.HPQCMacroConfigurationClass")

	if(null == customObject){
	    wikiname = "xwiki"
	    customObject =
		    xwiki.getDocument("$wikiname:HPQCMacro.HPQCMacroConfiguration").getObject("$wikiname:HPQCMacro.HPQCMacroConfigurationClass")
	}


	HPQC_HOST_URL = customObject.getProperty("hpqcUrl").value
	HPQC_PROJECT_USER = customObject.getProperty("hpqcUserName").value
	HPQC_PROJECT_PASS = customObject.getProperty("hpqcUserPass").value


	def params ="$xcontext.macro.params.params"

	/**
	 * Fetching the data from HPQC.
	 */
	def hpqcMacro = new HPQCMacro(HPQC_HOST_URL, HPQC_DOMAIN_NAME, HPQC_PROJECT_NAME, HPQC_PROJECT_USER, HPQC_PROJECT_PASS)

	try {
	    def issuesList = hpqcMacro.getIssuesList(params)

	    /**
	     * Pass the information to the HPQC API.
	     */
	    def tableSortableCfg = "(% class=\"sortable filterable doOddEven\" id=\"HPQCMacro\" %)"
	    def tableHeader ="(% class=\"sortHeader\" %)|= ID |= Tracker |= Status |= Thema "

	    println tableSortableCfg
	    println tableHeader

	    for(entry in issuesList){

		if(closedStatusValue.equalsIgnoreCase(entry.get("status"))) {
		    println "| --{{html}}<a href=\"${entry.get('macro-hyperlink')}\">#${entry.get('id')}</a>{{/html}}-- | --{{{${entry.get('status')}}}}-- | --{{{${entry.get(HPQC_ISSUE_CUSTOM)}}}}-- | --{{{${entry.get('name')}}}}--"
		}else {
		    println "| {{html}}<a href=\"${entry.get('macro-hyperlink')}\">#${entry.get('id')}</a>{{/html}} | {{{${entry.get('status')}}}} | {{{${entry.get(HPQC_ISSUE_CUSTOM)}}}} | {{{${entry.get('name')}}}}"
		}
	    }
	} catch( Exception e ){
	    println "Something went wrong. Please contact the developer: ${e}"
	    println "{{error}} {{{ $e }}} {{/error}}"
	}
    }
}