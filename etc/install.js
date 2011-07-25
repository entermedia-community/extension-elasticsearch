importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );

var war = "http://dev.entermediasoftware.com/jenkins/job/entermedia-elasticsearch/lastSuccessfulBuild/artifact/deploy/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.add("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.add("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

log.add("3. REPLACE LIBS");
var files = new FileUtils();
files.deleteMatch( web + "/lib/entermedia-cumulus*.jar");
files.deleteMatch( web + "/lib/Cumulus*.jar");
files.deleteMatch( web + "/base/entermedia/catalog/events/scripts/creimport.bsh");
files.deleteMatch( web + "/base/entermedia/catalog/events/runcreimport.xconf");


files.copyFileByMatch( tmp + "/WEB-INF/lib/entermedia-cumulus*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/entermedia/catalogs/testcatalog/events/scripts/creimport.bsh", web + "/base/entermedia/catalogs/testcatalog/events/scripts/");
files.copyFileByMatch( tmp + "/entermedia/catalogs/testcatalog/events/runcreimport.xconf",web + "/base/entermedia/catalogs/testcatalog/events/scripts/");

log.add("5. CLEAN UP");
files.deleteAll(tmp);

log.add("6. UPGRADE COMPLETED");