# Eugen

Eugen is the successor to the sadly abandoned [Kilian](https://github.com/rechen-werk/Kilian) Discord Bot. After facing some issues with the old codebase, we decided to start from scratch and build a new bot in a more suited and modern language that is based on the JVM. Eugen is written in Kotlin instead of Python and should provide the same features and fix some issues that were present in Kilian.

# Usage
**Note:** Commands marked with a :safety_vest: are only available to users added in the `managers.txt` file.

## /sleep :safety_vest:
Gracefully puts Eugen to sleep. All remaining queued messages are sent before turning off.

## /kusss \<url\> [mat-nr]
Subscribes the user to the Eugen Service. 
* The `url` is the link to the calendar file of [KUSSS](https://kuss.jku.at). The data is fetched, parsed and used to assign the corresponding LVA channels.
* The optional `mat-nr` is the student's matrikel number. At this point it is optional but may be useful to know at some occasion.

## /unkusss
Unsubscribes the user from the Eugen Service. Furthermore, any user-specific data stored is deleted.

## /reload :safety_vest:
Reloads the data from the calendar file. This action is also performed automatically every 24 hours.



# Requirements
TODO