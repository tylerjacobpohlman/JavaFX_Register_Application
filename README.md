# JavaFX_Register_Application

## Contact
I am unlikely to check any messages or comments via. GitHub. Please reach out to via. email
at [tpohlman@proton.me](tpohlman@proton.me) if there's any bugs, questions, concerns, or
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
<br />

Download and install [MySQL Sever](https://dev.mysql.com/downloads/mysql/). The latest version should be fine but, if 
you have any capability issues, download version *9.0.1*. If you have any other issues or 
need further clarification, read the [official MySQL Server installation guide](https://dev.mysql.com/doc/refman/9.0/en/installing.html).
Make sure to write down the root password you create during installation!
<br />
Next, download and install [MySQL Workbench](https://dev.mysql.com/downloads/workbench/). The latest version should also
be fine, but download version *8.0.38* if there's any capability issues. Once again, if you have any other issues or
need further clarification, read the [official MySQL Workbench installation guide](https://dev.mysql.com/doc/workbench/en/wb-installing.html).
<br />
Now download and install [IntelliJ IDE](https://www.jetbrains.com/idea/download/other.html). Follow the 
[official IntelliJ IDE installation guide](https://www.jetbrains.com/help/idea/installation-guide.html#snap) for further
information.
<br />
Depending on your operating system, you may or may not have Git already installed. If not, follow the 
[official Git download link](https://git-scm.com/downloads).
   <br />
Finally, download the [JDBC driver](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html). Make sure 
to download *ojdbc11.jar* to ensure compatibility with the most current version of Java JDK. Make sure to remember where 
this file is located as it is used in the next step.

2. **Clone the repository**:
   <br />
Open the applicable terminal for your operating system and navigate to the desired location where you want to copy the
project. Then, simply copy and paste the following command.
   ```bash 
   git clone https://github.com/tylerjacobpohlman/JavaFX_Register_Application
   ```
3. **Set up the database**:
<br />
Open MySQL Workbench. You should see *Local Instance 3306* under *MySQL Connections*.

![MySQL_Workbench_1.png](Setup%20Images/MySQL_Workbench_1.png)

Double-click on the instance and type in the root password you made at setup. Then, simply click **ok**.
![MYSQL_Workbench_2.png](Setup%20Images/MYSQL_Workbench_2.png)

You should now be on a screen which looks like the following:
![MYSQL_Workbench_3.png](Setup%20Images/MYSQL_Workbench_3.png)

Now click the folder icon in the top right corner to find an SQL script to run. Simply navigate to the cloned project
folder and, under the *SQL Scripts* subfolder, open the file *create_database_hvs.sql*. The window should now look as
the following.
![MYSQL_Workbench_4.png](Setup%20Images/MYSQL_Workbench_4.png)

Finally, run the script using the left-most lighting bolt icon. The database should now be setup and running.
![MYSQL_Workbench_5.png](Setup%20Images/MYSQL_Workbench_5.png)

3. **Setup the JavaFX application*:
