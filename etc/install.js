importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );

var war = "http://dev.entermediasoftware.com/jenkins/job/@BRANCH@extension-elasticsearch/lastSuccessfulBuild/artifact/deploy/extension-elasticsearch.zip";

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
files.deleteMatch( web + "/lib/extension-elasticsearch*.jar");
files.deleteMatch( web + "/lib/jline-*.jar");
files.deleteMatch( web + "/lib/jna-*.jar");
files.deleteMatch( web + "/lib/jts-*.jar");
files.deleteMatch( web + "/lib/elasticsearch-*.jar");
files.deleteMatch( web + "/lib/lucene-codecs-*.jar");
files.deleteMatch( web + "/lib/lucene-highlighter-*.jar");
files.deleteMatch( web + "/lib/lucene-join-*.jar");
files.deleteMatch( web + "/lib/lucene-queries-*.jar");
files.deleteMatch( web + "/lib/lucene-sandbox-*.jar");
files.deleteMatch( web + "/lib/lucene-spatial-*.jar");
files.deleteMatch( web + "/lib/lucene-memory-*.jar");
files.deleteMatch( web + "/lib/spatial4j-*.jar");


files.copyFileByMatch( tmp + "/lib/extension-elasticsearch*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/jna-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/jts-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/elasticsearch-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-codecs-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-highlighter-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-join-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-queries-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-sandbox-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-spatial-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/lucene-memory-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/lib/spatial4j-*.jar", web + "/lib/");



log.add("5. CLEAN UP");
files.deleteAll(tmp);

log.add("6. UPGRADE COMPLETED");
