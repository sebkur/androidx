#!/bin/bash
#
# Script to fetch generated API references docs from the Android build server and stage them.

source gbash.sh || exit

readonly defaultDb=""
DEFINE_string buildId --required "" "The build ID from the Android build server"
DEFINE_string dateStr "<insert date here>" "Date string used for CL message. Enclose date in double quotes (ex: \"April 29, 2021\")"
DEFINE_string db "$defaultDb" "The database used for staging. Omitting this value will stage changes to the staging DB."
DEFINE_bool useToT false "Stage docs from tip-of-tree docs build rather than public docs build"

gbash::init_google "$@"

# Allowlist for including specific directories being generated by Dackka.
#
# There are separate lists for Java and Kotlin refdocs as some libraries (such
# as Compose) only publish refdocs for a single language.
#
# Each directory's spelling must match the library's directory in
# frameworks/support.
#
# This list should match, or be a subset of, the list of libraries defined in
# https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/docs/AndroidXDocsPlugin.kt;l=568
readonly javaLibraryDirs=(
#  "benchmark"
#  "collection"
  "navigation"
  "paging"
  "wear"
  "window"
)
readonly kotlinLibraryDirs=(
#  "benchmark"
  "compose"
#  "collection"
  "navigation"
  "paging"
  "wear"
  "window"
)


# Change directory to this script's location and store the directory
cd "$(dirname $0)"
scriptDirectory=$(pwd)

# Working directories for the refdocs
outDir="$scriptDirectory/out"
readonly newDir="reference-docs"
readonly dackkaNewDir="reference-docs-dackka"

# Remove and recreate the existing out directory to avoid conflicts from previous runs
rm -rf $outDir
mkdir -p $outDir/$newDir
mkdir -p $outDir/$dackkaNewDir
cd $outDir

printf "=================================================================== \n"
printf "== Download the doc zip files from the build server \n"
printf "=================================================================== \n"

if (( FLAGS_useToT )); then
  printf "Downloading docs-tip-of-tree zip files \n"
  androidxJavaDocsZip="doclava-tip-of-tree-docs-${FLAGS_buildId}.zip"
  androidxKotlinDocsZip="dokka-tip-of-tree-docs-${FLAGS_buildId}.zip"
  androidxDackkaDocsZip="dackka-tip-of-tree-docs-${FLAGS_buildId}.zip"
else
  printf "Downloading docs-public zip files \n"
  androidxJavaDocsZip="doclava-public-docs-${FLAGS_buildId}.zip"
  androidxKotlinDocsZip="dokka-public-docs-${FLAGS_buildId}.zip"
  androidxDackkaDocsZip="dackka-public-docs-${FLAGS_buildId}.zip"
fi

/google/data/ro/projects/android/fetch_artifact --bid $FLAGS_buildId --target androidx $androidxJavaDocsZip
/google/data/ro/projects/android/fetch_artifact --bid $FLAGS_buildId --target androidx $androidxKotlinDocsZip
/google/data/ro/projects/android/fetch_artifact --bid $FLAGS_buildId --target androidx $androidxDackkaDocsZip

printf "\n"
printf "=================================================================== \n"
printf "== Unzip the doc zip files \n"
printf "=================================================================== \n"

unzip $androidxJavaDocsZip -d $newDir
unzip $androidxKotlinDocsZip -d $newDir
unzip $androidxDackkaDocsZip -d $dackkaNewDir

printf "\n"
printf "=================================================================== \n"
printf "== Format the doc zip files \n"
printf "=================================================================== \n"

cd $newDir

# Remove directories we never publish
rm en -rf
rm reference/java -rf
rm reference/org -rf
rm reference/hierarchy.html
rm reference/kotlin/org -rf

# Move package list into the correct location
mv reference/kotlin/package-list reference/kotlin/androidx/package-list

# Remove javascript files that have no use
rm -f reference/androidx/lists.js
rm -f reference/androidx/navtree_data.js

# Remove extraneous _book.yaml that improperly overwrites the correct one
rm -f reference/androidx/_book.yaml

printf "\n"
printf "=================================================================== \n"
printf "== Copy over Dackka generated refdocs \n"
printf "=================================================================== \n"

cd $outDir
for dir in "${javaLibraryDirs[@]}"
do
  printf "Copying Java refdocs for $dir\n"
  cp -r $dackkaNewDir/reference/androidx/$dir $newDir/reference/androidx/
done

for dir in "${kotlinLibraryDirs[@]}"
do
  printf "Copying Kotlin refdocs for $dir\n"
  cp -r $dackkaNewDir/reference/kotlin/androidx/$dir $newDir/reference/kotlin/androidx/
done

printf "Copying over Table of Contents and package lists"
cp $dackkaNewDir/reference/androidx/_toc.yaml $newDir/reference/androidx/
cp $dackkaNewDir/reference/androidx/package-list $newDir/reference/androidx/
cp $dackkaNewDir/reference/androidx/packages.html $newDir/reference/androidx/
cp $dackkaNewDir/reference/kotlin/androidx/_toc.yaml $newDir/reference/kotlin/androidx/
cp $dackkaNewDir/reference/kotlin/androidx/package-list $newDir/reference/kotlin/androidx/
cp $dackkaNewDir/reference/kotlin/androidx/packages.html $newDir/reference/kotlin/androidx/

# Copy over Dackka generated refdoc files that aren't generated by either
# Doclava or legacy Dokka (such as Java refdocs based on Kotlin sources)
rsync -avh --ignore-existing $dackkaNewDir/reference/ $newDir/reference/

printf "\n"
printf "=================================================================== \n"
printf "== Generate the language switcher \n"
printf "=================================================================== \n"

cd $newDir/reference
python3 ./../../../switcher.py --work androidx
python3 ./../../../switcher.py --work support

printf "\n"
printf "=================================================================== \n"
printf "== Create (if needed) and sync g4 workspace \n"
printf "=================================================================== \n"

client="$(p4 g4d -f androidx-ref-docs-stage)"
cd "$client"

# Revert all local changes to prevent merge conflicts when syncing.
# This is OK since we always want to start with a fresh CitC client
g4 revert ...
g4 sync

# Temporarily skipping due to o/128063951. b/186655027 tracks adding this back.
# TODO: check this logic when uncommenting
#
# Provision database if the target DB is not the default staging DB.
# if [ "${FLAGS_db}" != "$defaultDb" ]; then
#   printf "\n"
#   printf "=================================================================== \n"
#   printf "== Provision staging database ${FLAGS_db} \n"
#   printf "=================================================================== \n"
#
#   /google/data/ro/projects/devsite/devsite2 provision --db="${FLAGS_db}"
# fi

printf "\n"
printf "=================================================================== \n"
printf "== Prep directories and copy refdocs to CitC client \n"
printf "=================================================================== \n"

cd third_party/devsite/android/en/reference

cd kotlin/androidx
ls | grep -v "package\|class\|book\|toc\|constraint\|test\|index\|redirects" | xargs -I {} rm -rf {}
cd ../../androidx
ls | grep -v "package\|class\|book\|toc\|constraint\|test\|index\|redirects" | xargs -I {} rm -rf {}
cd ..

cp -r $outDir/$newDir/reference/* .

printf "\n"
printf "=================================================================== \n"
printf "== Create a changelist of pending refdoc changes \n"
printf "=================================================================== \n"

stagingLinkJava="go/dac-stage/reference/androidx/packages"
stagingLinkKotlin="go/dac-stage/reference/kotlin/androidx/packages"

# Add the db param to links if the target database is not the default staging DB.
if [ "$FLAGS_db" != "$defaultdb" ]; then
  stagingLinkJava+="?db=$FLAGS_db"
  stagingLinkKotlin+="?db=$FLAGS_db"
fi

# Construct CL description
clDesc="Androidx $FLAGS_dateStr Ref Docs

DO NOT SUBMIT

GO LIVE TIME: $FLAGS_dateStr @ 10:00 AM PST

Staged:
* Java: $stagingLinkJava
* Kotlin: $stagingLinkKotlin

All docs build id: $FLAGS_buildId

The following scripts were used to create these docs:

https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/referenceDocs/
"

# Grab the CL number generated from running `g4 change`.
clNum=$(g4 change --desc "$clDesc" | tail -1 | awk '{print $2}')
printf "View pending changes at http://cl/${clNum} \n"

printf "\n"
printf "=================================================================== \n"
printf "== Stage changes \n"
printf "=================================================================== \n"

# Construct the devsite command and parameters
devsiteCmd="/google/data/ro/projects/devsite/devsite2 stage"
devsiteCmd+=" --parallelize_build"
devsiteCmd+=" --use_large_thread_pools"
devsiteCmd+=" --upload_safety_check_mode=ignore"

# Add the --db flag if the target database is not the default staging DB.
if [ "$FLAGS_db" != "$defaultDb" ]; then
  devsiteCmd+=" --db=$FLAGS_db"
fi

# Directories to stage
devsiteCmd+=" android/support"
devsiteCmd+=" androidx"
devsiteCmd+=" kotlin/android/support"
devsiteCmd+=" kotlin/androidx"

printf "Running devsite command:\n"
printf "$devsiteCmd\n"

$devsiteCmd

# Print devsite command and CL link again in case they scrolled off the screen or
# scrollback buffer
printf "\n"
printf "Ran devsite command:\n"
printf "$devsiteCmd\n"
printf "\n"
printf "View pending changes at http://cl/${clNum} \n"
