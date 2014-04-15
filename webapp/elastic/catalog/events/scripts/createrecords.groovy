import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive

public void init(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher testsearcher = archive.getSearcher("testing");
	for(int i = 0; i < 10000000; i++){
		Data newdata = testsearcher.createNewData();
		newdata.setId("q"+ i);
		newdata.setProperty("fulltext", "Lavish much sold past on turtle oh around lucid that and into the along gallant dog much blinked dove gosh moth ladybug. Oh yikes or patted fruitfully jeepers forgetful hey sheep dear some giggled blamelessly this fanatic inside lynx got rhythmically removed oh thus other reindeer. Satanically pinched overcame overheard much when dismal one icy camel bravely ecstatic while until wisely within because overlaid in one improper that darn spoke less a jeez exactly creepy black editorial after belched oh. Antelope alas less behind jeeringly this huge much wrong classically consoled talkatively into mindfully wow during during inconspicuously a due one resold circuitously the less vaguely among that frog. Sadly humanely fastidious much behind desolately impulsive thus superbly much the lighted porcupine gallant gauche as altruistic much oh morbidly seagull desirable via black wow.");
		testsearcher.saveData(newdata, null);
	}	
	
	
}

init();