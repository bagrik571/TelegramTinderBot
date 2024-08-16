package com.tinderbot.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class TinderBotApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "user_telegram_tinder_bot";
    public static final String TELEGRAM_BOT_TOKEN = "7381204412:AAHdMhu-tRR383y-n3fx7yR-Z7OTUBtQjyA";
    public static final String OPEN_AI_TOKEN = "gpt:1puHA8mV14KI7xjh0h77JFkblB3TiTs00myoMUFQUScecINB";
    public DialogMode mode = DialogMode.MAIN;
    private List<String> chat;
    private UserInfo myInfo;
    private UserInfo personInfo;
    public static String waiting = "ChatGPT друкує.......";
    private int questionNumber;

    public ChatGPTService gptService = new ChatGPTService(OPEN_AI_TOKEN);

    public TinderBotApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();

        switch (message) {
            case "/start" -> {
                mode = DialogMode.MAIN;

                showMainMenu(
                        "головне меню бота", "/start",
                        "генерація Tinder-профілю 😎", "/profile",
                        "повідомлення для знайомства 🥰", "/opener",
                        "листування від вашого імені 😈", "/message",
                        "листування із зірками 🔥", "/date",
                        "поставити запитання чату GPT GPT 🧠", "/gpt");

                String menu = loadMessage("main");
                sendPhotoMessage("main");
                sendTextMessage(menu);
                return;
            }

            case "/gpt" -> {
                mode = DialogMode.GPT;

                sendPhotoMessage("gpt");
                String gptMessage = loadMessage("gpt");
                sendTextMessage(gptMessage);
                return;
            }

            case "/date" -> {
                mode = DialogMode.DATE;

                sendPhotoMessage("date");
                String dateMessage = loadMessage("date");
                sendTextButtonsMessage(dateMessage,
                        "Аріана Гранде 🔥", "date_grande",
                        "Марго Роббі 🔥🔥", "date_robbie",
                        "Зендея 🔥🔥🔥", "date_zendaya",
                        "Райан Гослінг 😎", "date_gosling",
                        "Том Харді 😎😎", "date_hardy");

                return;
            }

            case "/profile" -> {
                mode = DialogMode.PROFILE;

                sendPhotoMessage("profile");
                String profileMessage = loadMessage("profile");
                sendTextMessage(profileMessage);

                myInfo = new UserInfo();
                questionNumber = 1;
                sendTextMessage("Введіть ім'я");
                return;
            }
            case "/message" -> {
                mode = DialogMode.MESSAGE;

                sendPhotoMessage("message");
                String gptMessageHelper = loadMessage("message");
                sendTextButtonsMessage(gptMessageHelper,
                        "Наступне повідомлення", "message_next",
                        "Запросити на побачення", "message_date");

                chat = new ArrayList<>();
                return;
            }
            case "/opener" -> {
                mode = DialogMode.OPENER;

                sendPhotoMessage("opener");
                String openerMessage = loadMessage("opener");
                sendTextMessage(openerMessage);

                personInfo = new UserInfo();
                questionNumber = 1;
                sendTextMessage("Введіть ім'я");
                return;
            }
            default -> {
            }
        }

        switch (mode) {
            case MESSAGE -> {
                String query = getCallbackQueryButtonKey();

                if (query.startsWith("message_")) {
                    String prompt = loadPrompt(query);
                    String history = String.join("/n/n", chat);

                    Message msg = sendTextMessage(waiting);

                    String answer = gptService.sendMessage(prompt, history);
                    updateTextMessage(msg, answer);
                }

                chat.add(message);
            }
            case GPT -> {
                String prompt = loadPrompt("gpt");
                Message msg = sendTextMessage(waiting);

                String answer = gptService.sendMessage(prompt, message);
                updateTextMessage(msg, answer);
            }
            case DATE -> {
                String query = getCallbackQueryButtonKey();

                if (query.startsWith("date_")) {
                    sendPhotoMessage(query);
                    String prompt = loadPrompt(query);
                    gptService.setPrompt(prompt);
                    return;
                }
                Message msg = sendTextMessage(waiting);

                String answer = gptService.addMessage(message);
                updateTextMessage(msg, answer);
            }
            case PROFILE -> {
                if (questionNumber <= 6) {
                    askQuestion(message, myInfo, "profile");
                }
            }
            case OPENER -> {
                if (questionNumber <= 6) {
                    askQuestion(message, personInfo, "opener");
                }
            }
            default -> {
            }
        }

    }

    private void askQuestion(String message, UserInfo user, String profileName) {
        switch (questionNumber) {
            case 1 -> {
                user.name = message;
                questionNumber = 2;
                sendTextMessage("Введіть вік");
            }
            case 2 -> {
                user.age = message;
                questionNumber = 3;
                sendTextMessage("Введіть місто");
            }
            case 3 -> {
                user.city = message;
                questionNumber = 4;
                sendTextMessage("Введіть професію");
            }
            case 4 -> {
                user.occupation = message;
                questionNumber = 5;
                sendTextMessage("Введіть хоббі");
            }
            case 5 -> {
                user.hobby = message;
                questionNumber = 6;
                sendTextMessage("Введіть цілі для знайомства?");
            }
            case 6 -> {
                user.goals = message;
                String prompt = loadPrompt(profileName);
                Message msg = sendTextMessage(waiting);

                String answer = gptService.sendMessage(prompt, user.toString());
                updateTextMessage(msg, answer);

            }
            default -> {
                questionNumber = 1; // Resetting the process
                sendTextMessage("Щось пішло не так. Почнемо спочатку. Введіть ім'я.");
            }
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBotApp());
    }
}
