package green_green_avk.anotherterm.whatsnew;

import java.util.Date;

import green_green_avk.anotherterm.R;

final class Info {
    private Info() {
    }

    static final WhatsNewDialog.Entry[] news;

    static {
        news = new WhatsNewDialog.Entry[]{
                new WhatsNewDialog.Entry(R.string.news_IIIv51,
                        Date.UTC(122, 3, 20, 0, 0, 1)),
                new WhatsNewDialog.Entry(R.string.news_IIIv53,
                        Date.UTC(122, 4, 24, 0, 0, 0)),
                new WhatsNewDialog.Entry(R.string.news_IIIv57,
                        Date.UTC(122, 8, 21, 0, 0, 0)),
                new WhatsNewDialog.Entry(R.string.news_IIIv59,
                        Date.UTC(122, 9, 25, 0, 0, 0))
        };
    }
}
