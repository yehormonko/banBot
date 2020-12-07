import org.telegram.telegrambots.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatCleaner {
    private SQLiteManager sqLiteManager;
    private Bot bot;

    public ChatCleaner(SQLiteManager sqLiteManager, Bot bot) {
        this.sqLiteManager = sqLiteManager;
        this.bot = bot;
        demonCleaner();
    }

    public void demonCleaner() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ArrayList<BanInfo> allBans = sqLiteManager.getAllBans();
                        for (BanInfo ban : allBans) {
                            if (!ban.getBanDate().isAfter(LocalDateTime.now())) {
                                bot.kickMember(new KickChatMember().
                                        setChatId(ban.getChatId()).
                                        setUserId(Integer.valueOf(ban.getUserId())));
                                sqLiteManager.tryToRemoveBan(ban.getChatId(), ban.getUserId());
                            }
                        }

                        Thread.sleep(60000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void notifyNewChatMembers(String chatId, List<User> newMembers) throws TelegramApiException {
        ChatInfo chatInfo = sqLiteManager.getChatInfo(chatId);
        if (!chatInfo.isReady()) return;
        for (User user : newMembers) {
            LocalDateTime now = LocalDateTime.now();
            if (chatInfo.getValue_type().equals("day")) {
                now = now.plusDays(chatInfo.getValue());
            } else if (chatInfo.getValue_type().equals("hours")) {
                now = now.plusHours(chatInfo.getValue());
            } else if (chatInfo.getValue_type().equals("minutes")) {
                now = now.plusMinutes(chatInfo.getValue());
            } else {
                return;
            }
            sqLiteManager.addPotentialBan(chatId, user.getId().toString(), now);
            bot.sendMessage(new SendMessage().setText("@" + user.getUserName() + " " + chatInfo.getGreet())
                    .setChatId(chatId));
        }

    }

    public void checkIfUnbanned(Message message) {
        sqLiteManager.tryToRemoveBan(message.getChatId().toString(), message.getFrom().getId().toString());
    }

    public SQLiteManager getSqLiteManager() {
        return sqLiteManager;
    }

    public void setSqLiteManager(SQLiteManager sqLiteManager) {
        this.sqLiteManager = sqLiteManager;
    }

}
