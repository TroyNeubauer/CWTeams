# CWTeams
Insert blabbery Chas paragraph here

# Getting started
In order to build and run Cake Wars Teams, you will need to install a few dependencies:

Java: Can be downlaoded here https://www.java.com/ES/download/. Java is the language of choice for this project 

Gradle: Can be downlaoded here https://gradle.org/install/. Gradle is used as the build system to compile the code and manage the dependencies for this project

Git: Can be downlaoded here https://git-scm.com/downloads. Git allows you to download the source code from this repository and get updates as new commits are pushed

# Setup
Once the dependencies are met, open a [command prompt](https://tutorial.djangogirls.org/en/intro_to_command_line) and type `git clone https://github.com/TroyNeubauer/CWTeams.git`. This will download the soource code to your local machine. If you get an error that git cant be found then double check that you added it your path or try restarting your terminal.
Run `cd CWTeams` to navigate to the source code

Next execute `gradlew.bat run` (windows) or `./gradlew run` (OSX and Linux) to compile and run the program. 


# Usage
This utility comes with many command line arguments that allow you to tweak the resulting teams to your liking.
You can specify additional agruments that effect how the program runs with `./gradlew run --args='INSERT PROGRAM ARGS HERE'`

Initally the program will fail because it doesnt know how many teams to generate. Use `--teams=X` to specity this value Eg. `gradlew.bat run --args='--teams=2'` on windows or `./gradlew run --args='--teams=2'` for non-windows. If the number of players doesnt evenly divide into the team count then the program will properly handle balencing 3v2's 5v6's etc.

The program will look for a ratings file named `cw.xlsx` in the current directory. This can be overridden with `--file=other_cake_wars_file.xlsx` to make the program read `other_cake_wars_file.xlsx` instead.

By default the program will only output a set of teams which are within +-1 rating of each other. This can be restricted or widened with `--max-deviation=0.5` to ensure that all matches include teams within +-0.5 RP for example.

To specify players who cannot be on the same team use `--seperate player1username:player2username player3username:player4username ...` More than one pair of players can be entered. In the example above none of the teams generated will include player1 and player2 on the same team or player3 and player4 on the same team, however player1 and player3 can be on the same team although this isnt guarnteed. This program treats player names as case insensitive.

## Exit states
Once the program generates 1000 valid teams for the given configurations, it will print the output sorted from least balanced to most balanced then exit. specifying `--limit=X` can be used to limit the output to X different team-sets. If the program is unable to generate new teams for a certain timeout (15 seconds) then it will print the valid teams it generated so far and exit. This can be changed using `--timeout=SECONDS`.


## Normal Usage examples

`./gradlew run --args='--limit=10000 --teams=5 --separate smexy100:awesomeyu smexy100:awesomeyu ertrterw:anthonyyu46 anthonyyu46:thecleanerplate --output=teams.txt --timeout=5'`

The command above was used to generate 1000 different 5 man teams using the seperation rules, failing after 5 seconds of inactivity, and writing the final sorted team list to teams.txt.


## Full help output
usage: CWTeams [-h] [--file FILE] [--max-deviation MAXDEV] [--limit LIMITCOUNT] [--teams TEAMS] [--separate RESTRICTIONS [RESTRICTIONS ...]] [--output OUTPUTFILE] [--sort {true,false}]

An automated team balancing program made for cake wars by Troy Neubauer

named arguments:

  `-h, --help`             show this help message and exit

  `--file FILE, -f FILE`   Specifies the input file to get the player list and ranking matrix

  `--max-deviation MAXDEV, -m MAXDEV`
Indicates the max deviation in the total ranking across teams

  `--limit LIMITCOUNT, -l LIMITCOUNT`
Sets a limit for the max number of permeated teams to be generated

  `--teams TEAMS, -t TEAMS`
How many teams should be made from the bundle of players

  `--separate RESTRICTIONS [RESTRICTIONS ...], -r RESTRICTIONS [RESTRICTIONS ...]`
Indicates a binary seperation between two players using a colon. using `--separate troy:chas` will force the algorithm to make teams where chas and troy are on different teams

  `--output OUTPUTFILE, -o OUTPUTFILE`
Write the list of teams to the specified file

  `--sort {true,false}, -s {true,false}`
Waits until the program terminates to print the output (sorted from worst to best)

