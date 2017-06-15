package SentimentAnalysis;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import java.sql.*;
import java.util.*;

/**
 * Created by Robert on 13.03.2017.
 */
public class ConnectionBase {

    private Connection connection;
    private Statement statement;
    private static volatile ConnectionBase connect;
    private static final Logger log = LogManager.getLogger("Connection");


    private ConnectionBase()  {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            log.log(Level.ERROR,"Ошибка при записи данных в базу. (метод getInstance)",ex);
        }
        try {
            connection = DriverManager.getConnection("jdbc:SQLite:dbSentiment.db");
            statement = connection.createStatement();
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при создании подключения. (метод getInstance)",ex);
        }
        createTable();

    }
    private void createTable() {

        try {
            statement.execute("create table if not exists `collection` (\n" +
                    "\t`id`\tINTEGER NOT NULL UNIQUE,\n" +
                    "\t`name`\tTEXT NOT NULL,\n" +
                    "\t`tone`\tINTEGER NOT NULL,\n" +
                    "\t`size`\tINTEGER NOT NULL,\n" +
                    "\tPRIMARY KEY(`id`),\n" +
                    "\tFOREIGN KEY(`id`) REFERENCES `words`(`fieldID`));");
            statement.execute("create table if not exists `words` (\n" +
                    "\t`fieldID`\tINTEGER NOT NULL,\n" +
                    "\t`word`\tTEXT NOT NULL,\n" +
                    "\t`count`\tINTEGER NOT NULL\n" +
                    ");");
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при создании таблиц. (метод createTable)",ex);
        }
    }
    public static synchronized ConnectionBase getInstance() {
        if(connect == null){
                connect = new ConnectionBase();
        }
        return connect;
    }
    public void closeConnection() {
        try {
            statement.close();
            connection.close();
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при закрытии подключения к базе данных",ex);
        }
    }
    public synchronized void writeWordDB(HashMap<String, Integer> listWordWithCount, int fieldId)  {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("insert into words (fieldID, word, count) values ");
        for(Map.Entry<String, Integer> entry : listWordWithCount.entrySet()){
            stringBuffer.append("(")
                    .append(fieldId)
                    .append(",\"")
                    .append(entry.getKey())
                    .append("\", ")
                    .append(entry.getValue())
                    .append("),");
        }
        stringBuffer.deleteCharAt(stringBuffer.length()-1);
        stringBuffer.append(";");
        try {
            statement.execute(stringBuffer.toString());
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при записи данных в базу. Запись слов в базу (метод writeWordDB)",ex);
        }
    }
    public synchronized void writeGenreDB(String nameGenre, int tone, int size)  {
        try {
            ResultSet resultSet = statement.executeQuery("select * from collection where name = \"" + nameGenre + "\" and tone = " + tone);
            if (resultSet.next()){
                int id = resultSet.getInt("id");
                statement.execute("update collection set name = \"" + nameGenre + "\" , tone = " + tone +
                        ",size = " + size + " where id = " + id);
            }else{
                String string = "(\"" + nameGenre +"\"," + tone + "," + size + ")" ;
                statement.execute("insert into collection(name,tone,size) values" + string);
            }
            resultSet.close();
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при записи данных в базу. Запись жанра в базу (метод writeGenreDB)",ex);
        }

    }
    public int getCountWordDB(int id, String word) {
        int count = 1;
        try {
            ResultSet resultSet = statement.executeQuery("select * from words where word=\"" + word + "\" and fieldID =" + id);
            if(resultSet.next()){
                count =  resultSet.getInt("count");
            }
            resultSet.close();

        } catch (SQLException ex) {

            log.log(Level.ERROR,"Ошибка при получения данных. (метод getCountWordDB)",ex);
           }
        return count;
    }
    public ArrayList<String> getListNameGenreDBWithOutMorf() {
        ArrayList<String> listNameGenre = new ArrayList<>();
        try {
            ResultSet resultSet = statement.executeQuery("select * from collection");
            while (resultSet.next()){
                String name = resultSet.getString("name");
                if(!listNameGenre.contains(name)){
                    listNameGenre.add(name);
                }
            }
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при получения списка жанров. (метод getListNameGenreDBWthOutMorf)",ex);
        }
        return listNameGenre;
    }
    public synchronized int getGenreSize(String name, int tone) {
        int size = 0;
        try {
            ResultSet resultSet = statement.executeQuery("select * from collection where name = \"" + name + "\" and tone = " + tone);
            if(resultSet.next()){
                size = resultSet.getInt("size");
            }
            resultSet.close();
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при получения жанрa. (метод getGenreSize)",ex);
        }
        return size;
    }
    public synchronized int getId(String nameGenre, int tone) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("select * from collection where name = \"")
                .append(nameGenre )
                .append("\" and tone = ")
                .append(tone);
        int id = 99999;
        try {
            ResultSet resultSet = statement.executeQuery(stringBuffer.toString());
            if(resultSet.next()){
                id = resultSet.getInt("id");
            }
            resultSet.close();
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при получения жанрa. (метод getGenreSize)",ex);
        }
        return id;
    }
    public boolean deleteGenre(String nameGenre, int useMorf) {
        try {
            statement.execute("delete from words where fieldID = " + getId(nameGenre,1));
            statement.execute("delete from words where fieldID = " + getId(nameGenre,0));
            statement.execute("delete from collection where name = \"" + nameGenre + "\" " + " and useMorf=" + useMorf);
            return true;
        } catch (SQLException ex) {
            log.log(Level.ERROR,"Ошибка при удаления жанрa. (метод deleteGenre)",ex);
            return false;
        }
    }
    public HashMap<String, Integer> getHashMap(String nameGenre, int tone){
        HashMap<String,Integer> listCount = new LinkedHashMap<>();
        try {
            ResultSet resultSet = statement.executeQuery("select * from words where fieldID=" + getId(nameGenre,tone));
            while (resultSet.next()){
                listCount.put(resultSet.getString("word"),resultSet.getInt("count"));
            }
        } catch (SQLException ex) {
            log.log(Level.ERROR, "Ошибка при получение HashMap (getHashMap)",ex);
        }
        return listCount;
    }

}
