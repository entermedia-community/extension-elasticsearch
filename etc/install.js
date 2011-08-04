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
files.deleteMatch( web + "/lib/jline-*.jar");
files.deleteMatch( web + "/lib/jna-*.jar");
files.deleteMatch( web + "/lib/elasticsearch-*.jar");
files.deleteMatch( web + "/lib/lucene-analyzers-*.jar");
files.deleteMatch( web + "/lib/lucene-core-*.jar");
files.deleteMatch( web + "/lib/lucene-highlighter-*.jar");
files.deleteMatch( web + "/lib/lucene-memory-*.jar");
files.deleteMatch( web + "/lib/lucene-queries-*.jar");

files.copyFileByMatch( tmp + "/WEB-INF/lib/jline-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/jna-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/elasticsearch-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/lucene-analyzers-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/lucene-core-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/lucene-highlighter-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/lucene-memory-*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/lucene-queries-*.jar", web + "/lib/");

log.add("5. CLEAN UP");
files.deleteAll(tmp);

log.add("6. UPGRADE COMPLETED");