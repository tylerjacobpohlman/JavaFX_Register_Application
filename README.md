# JavaFX_Register_Application

## Contact
I am unlikely to check any messages or comments via. GitHub. Please reach out to via. email
at [tpohlman@proton.me](tpohlman@proton.me) if there are any bugs, questions, concerns, or
clarifications about the setup.
## Overview
This project is a proof of concept, with a cash register program made
using JavaFX connecting to a MySQL database.

## Features
### Java
- **JavaFX**: Use of a graphical user interface for ease of use.
  - **FXML**: Uses clearly defined standard markup language for 
displaying the GUI and ease of changing it.
  - **Controllers**: Defined controller classes encapsulate the actions
of the GUI and allow for reusability.
- **Custom Error Handling**: Custom exception classes for throwing
unique and identifiable errors.
- **Compartmentalization and Abstraction**: Unique classes for handling
JDBC driver and database access allow for reusable and clean code.
### MySQL
- **Item Management**: Add, search, and manage items using UPC codes.
- **Member Lookup**: Effectively handle membership data in order to apply
- discounts and add to member purchase history.
- **Sales Transactions**: Process sales, calculate totals, and handle receipts with ease.
- **Database Integration**: Uses a MySQL database to store data and calculate receipts.

## Prerequisites
- Java JDK 11 or higher
- JavaFX SDK
- MySQL Server
- JDBC driver
## Recommended Software
- MySQL Workbench
- IntelliJ IDEA Community Edition

## Setup and Installation
The setup of this project involves using MySQL Workbench and IntelliJ IDEA Community Edition
for running it on your machine. Other applicable IDEs can be used at your own discretion. I'm 
walking through the setup on macOS, so some of the instructions may differ depending on your 
operating system. I would recommend doing a quick web search if you're confused at any of the steps.

1. **Download and install the necessary programs**:

Download and install [MySQL Sever](https://dev.mysql.com/downloads/mysql/). The latest version should be fine but, if 
you have any capability issues, download version *9.0.1*. If you have any other issues or 
need further clarification, read the [official MySQL Server installation guide](https://dev.mysql.com/doc/refman/9.0/en/installing.html).
Make sure to write down the root password you create during installation!

Next, download and install [MySQL Workbench](https://dev.mysql.com/downloads/workbench/). The latest version should also
be fine, but download version *8.0.38* if there are any capability issues. Once again, if you have any other issues or
need further clarification, read the [official MySQL Workbench installation guide](https://dev.mysql.com/doc/workbench/en/wb-installing.html).
<br />
Now download and install [IntelliJ IDE](https://www.jetbrains.com/idea/download/other.html). Follow the 
[official IntelliJ IDEA installation guide](https://www.jetbrains.com/help/idea/installation-guide.html#snap) for further
information.

Depending on your operating system, you may or may not have Git already installed. If not, follow the 
[official Git download link](https://git-scm.com/downloads).

2. **Clone the repository**:

Open the applicable terminal for your operating system and navigate to the desired location where you want to copy the
project. Then, simply copy and paste the following command.
   ```bash 
   git clone https://github.com/tylerjacobpohlman/JavaFX_Register_Application
   ```
3. **Set up the database**:

Open MySQL Workbench. You should see *Local Instance 3306* under *MySQL Connections*.

![MySQL_Workbench_1.png](Setup%20Images/MySQL_Workbench_1.png)

Double-click on the instance and type in the root password you made at setup. Then, simply click **ok**.
![MYSQL_Workbench_2.png](Setup%20Images/MYSQL_Workbench_2.png)

You should now be on a screen that looks like the following:
![MYSQL_Workbench_3.png](Setup%20Images/MYSQL_Workbench_3.png)

Now click the folder icon in the top right corner to find an SQL script to run. Simply navigate to the cloned project
folder and, under the *SQL Scripts* subfolder, open the file *create_database_hvs.sql*. The window should now look as
the following.
![MYSQL_Workbench_4.png](Setup%20Images/MYSQL_Workbench_4.png)

Finally, run the script using the left-most lighting bolt icon. The database should now be setup and running.
![MYSQL_Workbench_5.png](Setup%20Images/MYSQL_Workbench_5.png)

3. **Set up the JavaFX application**:

Open IntelliJ, click *open*, navigate to the project director, and open the project from there.
![IntelliJ_1.png](Setup%20Images/IntelliJ_1.png)

Click the folder icon in the left sidebar. Navigate down through the subdirectory *src* until reaching *RegisterApplication.java*.
Double-click the *.java* file to open it.
![IntelliJ_2.png](Setup%20Images/IntelliJ_2.png)

If prompted, download a JDK version. Version 21 or later should work.
![IntelliJ_3.png](Setup%20Images/IntelliJ_3.png)

Finally, it's time to connect the driver. I would recommend using the driver provided in this project. First click on
the gear icon in the top right and then click on *project structure*.
![IntelliJ_4.png](Setup%20Images/IntelliJ_4.png)

Above all the provided libraries, click the plus icon, click *Java*, navigate to the *lib* subdirectory, and 
open *mysql-connector-java-8.0.30.jar*.
![IntelliJ_5.png](Setup%20Images/IntelliJ_5.png)

Click *apply* and *ok* at the bottom of the window. Now back at the initial window, click the green *run* icon at the top.
Doing so should bring up the following screen:
![IntelliJ_6.png](Setup%20Images/IntelliJ_6.png)

All the details provided are the default login credentials. Upon pressing *enter*, this following screen is displayed:
![IntelliJ_7.png](Setup%20Images/IntelliJ_7.png)

4. **You're done!**:

Play around with application! The tables *items* and *members* with the *hvs* database contain the data to test the
application. Click through all the options and refresh the database to see what changes!

## Contributions

This project wouldn't have become a reality without the close help of my Java Programming professor 
[John Ostroske](https://www.linkedin.com/in/john-ostroske-1b49aa59/) and Database Systems professor 
[Kevin Lizanich](https://www.linkedin.com/in/kevin-lizanich-86442a101/). Both went above and beyond in teaching their 
respective courses and reviewing my codebase.




