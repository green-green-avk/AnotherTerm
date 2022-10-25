package green_green_avk.anotherterm.whatsnew;

import java.util.Date;

import green_green_avk.anotherterm.R;

final class Info {
    private Info() {
    }

    static final WhatsNewDialog.Entry[] news;

    static {
        news = new WhatsNewDialog.Entry[]{
                new WhatsNewDialog.Entry(R.string.news_IV_dev18,
                        Date.UTC(122, 3, 20, 0, 0, 0)),
                new WhatsNewDialog.Entry(R.string.news_IV_dev20,
                        Date.UTC(122, 4, 24, 0, 0, 0)),
                new WhatsNewDialog.Entry(R.string.news_IV_dev24,
                        Date.UTC(122, 8, 21, 0, 0, 0)),
                new WhatsNewDialog.Entry(R.string.news_IV_dev26,
                        Date.UTC(122, 9, 25, 0, 0, 0))
        };
    }
}
