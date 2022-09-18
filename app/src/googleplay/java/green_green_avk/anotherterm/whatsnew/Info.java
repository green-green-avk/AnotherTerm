package green_green_avk.anotherterm.whatsnew;

import java.util.Date;

import green_green_avk.anotherterm.R;

final class Info {
    private Info() {
    }

    static final WhatsNewDialog.Entry[] news;

    static {
        news = new WhatsNewDialog.Entry[]{
                new WhatsNewDialog.Entry(R.string.news_IIIv57,
                        Date.UTC(122, 8, 21, 0, 0, 0))
        };
    }
}
