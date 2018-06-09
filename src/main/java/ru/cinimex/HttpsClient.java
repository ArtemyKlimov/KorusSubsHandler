package ru.cinimex;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class HttpsClient {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
    private final static String AUTH_URL = "https://courier-api.esphere.ru/api/auth/logon";
    private final static String AUTH_URL_DEMO = "https://courier-demo.esphere.ru/api/auth/logon";
    private final static String ADD_GET_DELETE_SUB_URL_DEMO = "https://courier-demo.esphere.ru/api/v2.0/subscription";
    private final static String ADD_GET_DELETE_SUB_URL = "https://courier-api.esphere.ru/v2.0/subscription";
    private static boolean printToFile;
    private static boolean printStackTrace;
    private static boolean useDemoStand;
    public static void main(String[]args) throws FileNotFoundException{
        if (args.length == 0) {
            System.out.println(getHelpMessage());
            return;
        }
        if (args.length == 1) {
            if ((args[0].equalsIgnoreCase("-h")) || (args[0].equalsIgnoreCase("-help"))) {
                System.out.println(getHelpMessage());
                return;
            } else {
                System.out.println("Unknown argument \"" + args[0] + "\". Use \"-help\" to get user guide");
                return;
            }
        }
        String unknownArgument  = checkArguments(args);
        if ( unknownArgument != null) {
            System.out.println("Unknown argument \"" + unknownArgument + "\"");
            System.out.println(getHelpMessage());
            return;
        }

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-stacktrace")){
                printStackTrace = true;
            }else if (arg.equalsIgnoreCase("-f")){
                printToFile = true;
            } else if (arg.equalsIgnoreCase("-demo")) {
                useDemoStand = true;
            }
        }

        try {
            if (args[0].equals("-add")) {
                addSubscription(args);
            } else if (args[0].equals("-get")) {
                getSubscriptions(args);
            } else if ((args[0].equals("-del")) || (args[0].equals("-delete"))) {
                deleteSubscription(args);
            } else {
                System.out.println(getHelpMessage());
                return;
            }
        }catch (KorusException ke) {

            if (printStackTrace) {
                if (printToFile) {
                    File f = new File(getExceptionFileName());
                    PrintStream ps = new PrintStream(f);
                    ke.printStackTrace(ps);
                }else {
                    ke.printStackTrace();
                }
            }
        }
    }

    private static String checkArguments(String [] args) {
        String result = null;
        int startArg;
        if (args[0].equalsIgnoreCase("-del") || args[0].equalsIgnoreCase("-delete"))
            startArg = 6;
        else
            startArg = 5;
        for (int i = startArg; i < args.length; i++) {
            if (!(args[i].equalsIgnoreCase("-f")
                    || args[i].equalsIgnoreCase("-stacktrace")
                    || args[i].equalsIgnoreCase("-demo"))){
                result = args[i];
                break;
            }
        }

        return result;
    }


    HttpsClient(String url, String name, String password) throws Exception{  }


    private static String getSubscriptionToken(String url, String token) throws KorusException{
        HttpClient httpclient = new DefaultHttpClient();
        HttpPut httpPut = new HttpPut(url);
        httpPut.addHeader("Auth-token", token);
        httpPut.addHeader("Accept", "application/json");
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpclient.execute(httpPut);
        } catch (IOException ioe) {
            throw new KorusException("Can not establish connection with subscription/add method", ioe, 113);
        }
        int returnCode;
        String inputLine;
        try {
            InputStream connectionIn = httpResponse.getEntity().getContent();
            returnCode = httpResponse.getStatusLine().getStatusCode();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(connectionIn));
            inputLine = buffer.readLine();
            buffer.close();
        } catch (IOException ioe) {
            throw new KorusException("Can not get content of subscription/add method reply", ioe, 114);
        }
        String subscriptionToken = null;

        try {
            if ((returnCode > 199) && (returnCode < 300)) {
                JSONObject jsonObject = new JSONObject(inputLine);
                subscriptionToken = (String) jsonObject.get("token");
            } else {
                throw new KorusException("Unsuccessfull result of adding subscription; Error code = " + returnCode + "; Message: "+ inputLine, null, 116);
            }
        } catch (JSONException jse) {
            throw new KorusException("Can not parse JSON with subscription token: " + inputLine, jse, 115);
        }
        return subscriptionToken;
    }


    private static String getAuthToken(String url, String name, String password) throws KorusException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json");
        JSONObject auth = new JSONObject();
        auth.put("username", name);
        auth.put("password", password);
        String authtoken;
        String inputLine = null;
        try {
        StringEntity entity = new StringEntity(auth.toString());
        httpPost.setEntity(entity);
        HttpResponse httpResponse = null;

        httpResponse = httpclient.execute(httpPost);

        InputStream connectionIn = httpResponse.getEntity().getContent();

        BufferedReader buffer = new BufferedReader(new InputStreamReader(connectionIn));
        inputLine = buffer.readLine();
        buffer.close();

        JSONObject authReply = new JSONObject(inputLine);

        authtoken = (String) authReply.get("token");
        } catch (UnsupportedEncodingException uns) {
            throw new KorusException("Encoding problem occured", uns, 110);
        } catch (IOException ioe) {
            throw new KorusException("Can not establish http connection for authorization\n", ioe, 112);
        } catch (JSONException jse) {
            throw new KorusException("Can not parse JSON with authorization token:\n" + inputLine, jse, 115);
        }
        return authtoken;
    }


    private static String callSubscriptionsHpptRequest(String url, String authToken) throws KorusException{
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Auth-token", authToken);
        httpGet.addHeader("Accept", "application/json");
        HttpResponse httpResponse;
        try {
            httpResponse = httpclient.execute(httpGet);
        } catch (IOException ioe) {
            throw new KorusException("Can not establish connection with subscription/get method", ioe, 117);
        }

        String subscriptionToken = null;
        int returnCode;
        String inputLine;
        try {
            InputStream connection = httpResponse.getEntity().getContent();
            returnCode = httpResponse.getStatusLine().getStatusCode();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(connection));
            inputLine = buffer.readLine();
            buffer.close();
        } catch (IOException ioe) {
            throw new KorusException("Can not get content of subscription/get method reply", ioe, 114);
        }

        return inputLine;
    }

    private static String callSubscriptionDeleteHttpRequest(String url, String authToken, String subToken) throws KorusException{
        String deleteUrl = url + "/" + subToken;
        HttpClient httpclient = new DefaultHttpClient();
        HttpDelete httpDelete = new HttpDelete(deleteUrl);
        httpDelete.addHeader("Auth-token", authToken);
        httpDelete.addHeader("Accept", "application/json");
        HttpResponse httpResponse;
        try {
            httpResponse = httpclient.execute(httpDelete);
        } catch (IOException ioe) {
            throw new KorusException("Can not establish connection with subscription/delete method", ioe, 118);
        }
        int returnCode = httpResponse.getStatusLine().getStatusCode();
        if ((returnCode < 200) || (returnCode >299))
            return "Unsuccessful. Return code = " + returnCode;

        return "OK";
    }


    private static void addSubscription(String[] args) throws FileNotFoundException, KorusException {
        if ((args.length < 5 || (args.length > 8)) || !argumentsAreValid(args)) {
            System.out.println("Wrong number of arguments");
            System.out.println(getHelpMessage());
            return;
        }
        String currentAuthURL =  useDemoStand ? AUTH_URL_DEMO : AUTH_URL;
        String authToken = getAuthToken(currentAuthURL, args[2], args[4]);

        String currentSubURL = useDemoStand ? ADD_GET_DELETE_SUB_URL_DEMO : ADD_GET_DELETE_SUB_URL;
        String subscriptionToken = getSubscriptionToken(currentSubURL, authToken);

        if (printToFile) {
            File f = new File(getFileName(args[2]));
            PrintWriter out = new PrintWriter(f);
            out.println(subscriptionToken);
            out.close();
        } else
            System.out.println("Subscription token is:\n\r" + subscriptionToken);
    }

    private static void getSubscriptions(String[] args) throws KorusException, FileNotFoundException{
        if (!((args.length == 5) || (args.length == 6) || (args.length == 7)) || !argumentsAreValid(args)) {
            System.out.println("Wrong number of arguments");
            System.out.println(getHelpMessage());
            return;
        }
        String currentAuthURL =  useDemoStand ? AUTH_URL_DEMO : AUTH_URL;
        String authToken = getAuthToken(currentAuthURL, args[2], args[4]);
        String currentSubURL = useDemoStand ? ADD_GET_DELETE_SUB_URL_DEMO : ADD_GET_DELETE_SUB_URL;
        String subscriptions = callSubscriptionsHpptRequest(currentSubURL, authToken);

        if (printToFile == true) {
            File f = new File(getFileName(args[2]));
            PrintWriter out = new PrintWriter(f);
            out.println(subscriptions);
            out.close();
        } else
            System.out.println("Subscription token is:\n\r" + subscriptions);
    }

    private static void deleteSubscription(String[] args) throws KorusException, FileNotFoundException{
        if ((args.length < 6 || (args.length > 9)) || !argumentsAreValid(args)) {
            System.out.println("Wrong number of arguments");
            System.out.println(getHelpMessage());
            return;
        }

        String currentAuthURL =  useDemoStand ? AUTH_URL_DEMO : AUTH_URL;
        String authToken = getAuthToken(currentAuthURL, args[2], args[4]);

        String currentSubURL = useDemoStand ? ADD_GET_DELETE_SUB_URL_DEMO : ADD_GET_DELETE_SUB_URL;
        String result = callSubscriptionDeleteHttpRequest(currentSubURL, authToken, args[5]);


        if (printToFile) {
            File f = new File(getFileName(args[2]));
            PrintWriter out = new PrintWriter(f);
            out.println(result);
            out.close();
        } else
            System.out.println("Subscription removal successful:\n\r" + result);

    }

    private static boolean argumentsAreValid(String[] args) {
        return args[1].equalsIgnoreCase("-u") && args[3].equalsIgnoreCase("-p");
    }

    private static String getFileName(String userName) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return "KorusSub_" + userName + "_" + sdf.format(timestamp) + ".txt";
    }

    private static String getExceptionFileName() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return "KorusSubError_" + sdf.format(timestamp) + ".txt";
    }

    private static String getHelpMessage() {
        StringBuilder s = new StringBuilder();
        s.append("\n*** KorusSubscriptionHandler help ***\n");
        s.append("Available operations are: \n");
        s.append("add new subscription:\nKorusSubHandler -add -u <User> -p <Password>\n");
        s.append("get subscriptions:\nKorusSubHandler -get -u <User> -p <Password>\n");
        s.append("delete subscription:\nKorusSubHandler -del -u <User> -p <Password> <subscription>\n");
        s.append("optional arguments:\n");
        s.append("\"-f\"          to print result into a file\n");
        s.append("\"-stacktrace\" to see exception stacktrace\n");
        s.append("\"-demo\"       to execute script on demo stand\n");
        s.append("\"-h\"          to read manual again\n\n");
        s.append("*** Developed by AO \"Cinimex\" for AO \"Alfabank\" ***");
        return  s.toString();
    }
}
