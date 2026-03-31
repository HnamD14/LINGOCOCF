package com.example.auth;

import com.example.auth.model.*;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository         userRepo;
    private final PasswordEncoder        passwordEncoder;
    private final VocabularyRepository   vocabRepo;
    private final VocabSetRepository     setRepo;
    private final VocabSetItemRepository setItemRepo;

    @Override
    public void run(String... args) {
        seedAdmin();
        if (vocabRepo.count() == 0) {
            seedVocabularies();
        }
    }

    private void seedAdmin() {
        if (userRepo.findByUsername("admin").isEmpty()) {
            userRepo.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@lingococ.vn")
                .role(User.Role.ADMIN)
                .build());
            log.info("✅ Admin account created: admin / admin123");
        }
    }

    private void seedVocabularies() {

        // ══════════════════════════════════════════
        // HSK 1 — Miễn phí
        // ══════════════════════════════════════════

        List<Vocabulary> hsk1Greet = List.of(
            v("你好",   "nǐ hǎo",      "Xin chào",             1, "Chào hỏi",  "你好！很高兴认识你。",      "Xin chào! Rất vui được gặp bạn."),
            v("谢谢",   "xiè xiè",     "Cảm ơn",               1, "Chào hỏi",  "谢谢你帮助我。",           "Cảm ơn bạn đã giúp tôi."),
            v("对不起", "duì bu qǐ",   "Xin lỗi",              1, "Chào hỏi",  "对不起，我迟到了。",        "Xin lỗi, tôi đến muộn."),
            v("再见",   "zài jiàn",    "Tạm biệt",             1, "Chào hỏi",  "再见，明天见！",            "Tạm biệt, hẹn gặp ngày mai!"),
            v("你好吗", "nǐ hǎo ma",   "Bạn có khỏe không?",  1, "Chào hỏi",  "你好吗？我很好，谢谢。",    "Bạn khỏe không? Tôi rất khỏe, cảm ơn."),
            v("没关系", "méi guān xi", "Không sao",            1, "Chào hỏi",  "没关系，不用谢。",          "Không sao, không cần cảm ơn."),
            v("请",     "qǐng",        "Xin / Mời",            1, "Chào hỏi",  "请坐，请喝茶。",            "Mời ngồi, mời uống trà."),
            v("是",     "shì",         "Là / Đúng",            1, "Chào hỏi",  "是的，我是中国人。",        "Đúng vậy, tôi là người Trung Quốc.")
        );

        List<Vocabulary> hsk1Numbers = List.of(
            v("一", "yī",  "Một",  1, "Số đếm", "我有一个苹果。", "Tôi có một quả táo."),
            v("二", "èr",  "Hai",  1, "Số đếm", "她有两个孩子。", "Cô ấy có hai đứa con."),
            v("三", "sān", "Ba",   1, "Số đếm", "请等三分钟。",   "Vui lòng đợi ba phút."),
            v("四", "sì",  "Bốn",  1, "Số đếm", "今天是四号。",   "Hôm nay là ngày mồng bốn."),
            v("五", "wǔ",  "Năm",  1, "Số đếm", "我五岁开始学习。","Tôi bắt đầu học lúc năm tuổi."),
            v("六", "liù", "Sáu",  1, "Số đếm", "六月是夏天。",   "Tháng sáu là mùa hè."),
            v("七", "qī",  "Bảy",  1, "Số đếm", "一周有七天。",   "Một tuần có bảy ngày."),
            v("八", "bā",  "Tám",  1, "Số đếm", "八月很热。",     "Tháng tám rất nóng."),
            v("九", "jiǔ", "Chín", 1, "Số đếm", "九月开学了。",   "Tháng chín bắt đầu năm học."),
            v("十", "shí", "Mười", 1, "Số đếm", "他十岁了。",     "Anh ấy mười tuổi rồi.")
        );

        List<Vocabulary> hsk1Family = List.of(
            v("爸爸", "bà ba",    "Bố",       1, "Gia đình", "我爸爸是老师。",    "Bố tôi là giáo viên."),
            v("妈妈", "mā ma",    "Mẹ",       1, "Gia đình", "妈妈做饭很好吃。",  "Mẹ nấu ăn rất ngon."),
            v("哥哥", "gē ge",    "Anh trai", 1, "Gia đình", "我哥哥比我高。",    "Anh trai tôi cao hơn tôi."),
            v("姐姐", "jiě jie",  "Chị gái",  1, "Gia đình", "姐姐在北京工作。",  "Chị gái làm việc ở Bắc Kinh."),
            v("弟弟", "dì di",    "Em trai",  1, "Gia đình", "弟弟很调皮。",      "Em trai rất nghịch ngợm."),
            v("妹妹", "mèi mei",  "Em gái",   1, "Gia đình", "妹妹喜欢唱歌。",    "Em gái thích hát."),
            v("朋友", "péng yǒu", "Bạn bè",   1, "Gia đình", "他是我的好朋友。",  "Anh ấy là bạn tốt của tôi."),
            v("老师", "lǎo shī",  "Giáo viên",1, "Gia đình", "老师教我们汉语。",  "Giáo viên dạy chúng tôi tiếng Trung.")
        );

        List<Vocabulary> hsk1Food = List.of(
            v("水",   "shuǐ",     "Nước",    1, "Đồ ăn", "我想喝水。",       "Tôi muốn uống nước."),
            v("米饭", "mǐ fàn",   "Cơm",     1, "Đồ ăn", "我喜欢吃米饭。",   "Tôi thích ăn cơm."),
            v("面条", "miàn tiáo","Mì sợi",  1, "Đồ ăn", "我想吃面条。",     "Tôi muốn ăn mì sợi."),
            v("苹果", "píng guǒ", "Quả táo", 1, "Đồ ăn", "这个苹果很甜。",   "Quả táo này rất ngọt."),
            v("茶",   "chá",      "Trà",     1, "Đồ ăn", "你喝茶还是咖啡？", "Bạn uống trà hay cà phê?"),
            v("好吃", "hǎo chī",  "Ngon",    1, "Đồ ăn", "这道菜很好吃！",   "Món ăn này rất ngon!"),
            v("饿",   "è",        "Đói",     1, "Đồ ăn", "我很饿，想吃饭。", "Tôi rất đói, muốn ăn cơm."),
            v("渴",   "kě",       "Khát",    1, "Đồ ăn", "我渴了，想喝水。", "Tôi khát rồi, muốn uống nước.")
        );

        List<Vocabulary> hsk1Time = List.of(
            v("今天",   "jīn tiān",  "Hôm nay",       1, "Thời gian", "今天天气很好。",     "Hôm nay thời tiết rất đẹp."),
            v("明天",   "míng tiān", "Ngày mai",       1, "Thời gian", "明天我去北京。",     "Ngày mai tôi đến Bắc Kinh."),
            v("昨天",   "zuó tiān",  "Hôm qua",        1, "Thời gian", "昨天下雨了。",       "Hôm qua trời mưa."),
            v("现在",   "xiàn zài",  "Bây giờ",        1, "Thời gian", "现在几点了？",       "Bây giờ là mấy giờ rồi?"),
            v("上午",   "shàng wǔ",  "Buổi sáng",      1, "Thời gian", "上午我上课。",       "Buổi sáng tôi đi học."),
            v("下午",   "xià wǔ",    "Buổi chiều",     1, "Thời gian", "下午我打篮球。",     "Buổi chiều tôi chơi bóng rổ."),
            v("晚上",   "wǎn shàng", "Buổi tối",       1, "Thời gian", "晚上我看书。",       "Buổi tối tôi đọc sách."),
            v("星期一", "xīng qī yī","Thứ hai",        1, "Thời gian", "星期一我有汉语课。", "Thứ hai tôi có giờ học tiếng Trung.")
        );

        vocabRepo.saveAll(hsk1Greet);
        vocabRepo.saveAll(hsk1Numbers);
        vocabRepo.saveAll(hsk1Family);
        vocabRepo.saveAll(hsk1Food);
        vocabRepo.saveAll(hsk1Time);

        buildSet("HSK1 – Chào hỏi cơ bản",    "8 câu chào hỏi thiết yếu nhất cho người mới.",       1, "Chào hỏi",   false, hsk1Greet);
        buildSet("HSK1 – Số đếm 1-10",         "Đếm số từ 1 đến 10 bằng tiếng Trung.",               1, "Số đếm",     false, hsk1Numbers);
        buildSet("HSK1 – Gia đình",            "Các thành viên trong gia đình.",                      1, "Gia đình",   false, hsk1Family);
        buildSet("HSK1 – Đồ ăn & Đồ uống",    "Từ vựng về ăn uống hàng ngày.",                      1, "Đồ ăn",      false, hsk1Food);
        buildSet("HSK1 – Thời gian",           "Cách nói về thời gian: sáng, chiều, tối, hôm nay...",1, "Thời gian",  false, hsk1Time);

        // ══════════════════════════════════════════
        // HSK 2 — PLUS
        // ══════════════════════════════════════════

        List<Vocabulary> hsk2Travel = List.of(
            v("飞机", "fēi jī",    "Máy bay",    2, "Du lịch", "我坐飞机去上海。",   "Tôi đi máy bay đến Thượng Hải."),
            v("火车", "huǒ chē",   "Tàu hỏa",   2, "Du lịch", "火车票多少钱？",     "Vé tàu hỏa bao nhiêu tiền?"),
            v("地铁", "dì tiě",    "Tàu điện ngầm",2,"Du lịch","坐地铁很方便。",    "Đi tàu điện ngầm rất tiện lợi."),
            v("宾馆", "bīn guǎn",  "Khách sạn",  2, "Du lịch", "我住在宾馆里。",     "Tôi ở trong khách sạn."),
            v("护照", "hù zhào",   "Hộ chiếu",   2, "Du lịch", "请出示你的护照。",   "Vui lòng xuất trình hộ chiếu của bạn."),
            v("地图", "dì tú",     "Bản đồ",     2, "Du lịch", "我需要一张地图。",   "Tôi cần một tờ bản đồ."),
            v("旅游", "lǚ yóu",    "Du lịch",    2, "Du lịch", "我喜欢去旅游。",     "Tôi thích đi du lịch."),
            v("出发", "chū fā",    "Khởi hành",  2, "Du lịch", "我们明天出发吧。",   "Chúng ta khởi hành ngày mai nhé.")
        );

        List<Vocabulary> hsk2Work = List.of(
            v("工作", "gōng zuò", "Công việc",  2, "Công việc", "你的工作怎么样？",   "Công việc của bạn thế nào?"),
            v("公司", "gōng sī",  "Công ty",    2, "Công việc", "我在一家大公司工作。","Tôi làm việc ở một công ty lớn."),
            v("会议", "huì yì",   "Cuộc họp",   2, "Công việc", "下午有一个会议。",   "Buổi chiều có một cuộc họp."),
            v("电脑", "diàn nǎo", "Máy tính",   2, "Công việc", "我每天用电脑工作。", "Tôi làm việc trên máy tính mỗi ngày."),
            v("手机", "shǒu jī",  "Điện thoại", 2, "Công việc", "手机没电了。",       "Điện thoại hết pin rồi."),
            v("邮件", "yóu jiàn", "Email",      2, "Công việc", "我发了一封邮件。",   "Tôi đã gửi một email."),
            v("加班", "jiā bān",  "Làm thêm giờ",2,"Công việc", "今天要加班。",       "Hôm nay phải làm thêm giờ."),
            v("假期", "jià qī",   "Kỳ nghỉ",    2, "Công việc", "下周我有假期。",     "Tuần sau tôi có kỳ nghỉ.")
        );

        List<Vocabulary> hsk2Shopping = List.of(
            v("商店", "shāng diàn","Cửa hàng",  2, "Mua sắm", "我去商店买东西。",   "Tôi đến cửa hàng mua đồ."),
            v("便宜", "pián yí",   "Rẻ",        2, "Mua sắm", "这件衣服很便宜。",   "Bộ quần áo này rất rẻ."),
            v("贵",   "guì",       "Đắt",       2, "Mua sắm", "这个太贵了！",       "Cái này quá đắt!"),
            v("价格", "jià gé",    "Giá cả",    2, "Mua sắm", "这个价格合理吗？",   "Giá này có hợp lý không?"),
            v("付钱", "fù qián",   "Trả tiền",  2, "Mua sắm", "我用支付宝付钱。",   "Tôi trả tiền bằng Alipay."),
            v("打折", "dǎ zhé",    "Giảm giá",  2, "Mua sắm", "现在打八折。",       "Hiện đang giảm giá 20%."),
            v("衣服", "yī fu",     "Quần áo",   2, "Mua sắm", "我想买一件新衣服。", "Tôi muốn mua một bộ quần áo mới."),
            v("颜色", "yán sè",    "Màu sắc",   2, "Mua sắm", "你喜欢什么颜色？",   "Bạn thích màu gì?")
        );

        vocabRepo.saveAll(hsk2Travel);
        vocabRepo.saveAll(hsk2Work);
        vocabRepo.saveAll(hsk2Shopping);

        buildSet("HSK2 – Du lịch",     "Từ vựng du lịch: vé, tàu, máy bay, khách sạn...", 2, "Du lịch",    true, hsk2Travel);
        buildSet("HSK2 – Công việc",   "Từ vựng văn phòng, công việc hàng ngày.",          2, "Công việc",  true, hsk2Work);
        buildSet("HSK2 – Mua sắm",     "Mua sắm, trả tiền, mặc cả tiếng Trung.",           2, "Mua sắm",    true, hsk2Shopping);

        // ══════════════════════════════════════════
        // HSK 3 — PRO
        // ══════════════════════════════════════════

        List<Vocabulary> hsk3Emotions = List.of(
            v("高兴",  "gāo xìng",  "Vui mừng",      3, "Cảm xúc", "我很高兴见到你。",     "Tôi rất vui khi gặp bạn."),
            v("难过",  "nán guò",   "Buồn bã",        3, "Cảm xúc", "她很难过，哭了。",     "Cô ấy rất buồn, đã khóc."),
            v("生气",  "shēng qì",  "Tức giận",       3, "Cảm xúc", "他生气了，不说话。",   "Anh ấy tức giận, không nói chuyện."),
            v("担心",  "dān xīn",   "Lo lắng",        3, "Cảm xúc", "妈妈担心我的成绩。",   "Mẹ lo lắng về điểm số của tôi."),
            v("激动",  "jī dòng",   "Phấn khích",     3, "Cảm xúc", "我激动得说不出话。",   "Tôi phấn khích đến mức không nói được."),
            v("失望",  "shī wàng",  "Thất vọng",      3, "Cảm xúc", "考试没过，他很失望。", "Thi trượt, anh ấy rất thất vọng."),
            v("后悔",  "hòu huǐ",   "Hối hận",        3, "Cảm xúc", "我后悔没有努力学习。", "Tôi hối hận vì không học chăm chỉ."),
            v("骄傲",  "jiāo ào",   "Tự hào",         3, "Cảm xúc", "父母为我骄傲。",       "Bố mẹ tự hào về tôi.")
        );

        List<Vocabulary> hsk3Society = List.of(
            v("社会",  "shè huì",   "Xã hội",         3, "Xã hội", "我们生活在社会中。",   "Chúng ta sống trong xã hội."),
            v("环境",  "huán jìng", "Môi trường",     3, "Xã hội", "保护环境很重要。",     "Bảo vệ môi trường rất quan trọng."),
            v("文化",  "wén huà",   "Văn hóa",        3, "Xã hội", "中国文化很丰富。",     "Văn hóa Trung Quốc rất phong phú."),
            v("经济",  "jīng jì",   "Kinh tế",        3, "Xã hội", "中国经济发展很快。",   "Kinh tế Trung Quốc phát triển rất nhanh."),
            v("政府",  "zhèng fǔ",  "Chính phủ",      3, "Xã hội", "政府出台了新政策。",   "Chính phủ ban hành chính sách mới."),
            v("传统",  "chuán tǒng","Truyền thống",   3, "Xã hội", "春节是中国的传统节日。","Tết Nguyên Đán là lễ hội truyền thống Trung Quốc."),
            v("科技",  "kē jì",     "Khoa học công nghệ",3,"Xã hội","科技改变了我们的生活。","Khoa học công nghệ thay đổi cuộc sống của chúng ta."),
            v("发展",  "fā zhǎn",   "Phát triển",     3, "Xã hội", "城市发展越来越快。",   "Thành phố phát triển ngày càng nhanh.")
        );

        vocabRepo.saveAll(hsk3Emotions);
        vocabRepo.saveAll(hsk3Society);

        buildSet("HSK3 – Cảm xúc & Tâm trạng", "Diễn đạt cảm xúc phức tạp bằng tiếng Trung.", 3, "Cảm xúc", true, hsk3Emotions);
        buildSet("HSK3 – Xã hội & Văn hóa",     "Từ vựng về xã hội, văn hóa, kinh tế.",         3, "Xã hội",  true, hsk3Society);

        log.info("✅ Seeded {} vocabularies + 10 vocab sets (HSK1×5, HSK2×3, HSK3×2)", vocabRepo.count());
    }

    private void buildSet(String name, String desc, int level, String topic,
                          boolean premium, List<Vocabulary> vocabs) {
        VocabSet set = setRepo.save(VocabSet.builder()
            .name(name).description(desc)
            .hskLevel(level).topic(topic).isPremium(premium)
            .build());
        for (int i = 0; i < vocabs.size(); i++) {
            setItemRepo.save(VocabSetItem.builder()
                .vocabSet(set).vocabulary(vocabs.get(i)).orderIndex(i).build());
        }
    }

    private Vocabulary v(String hanzi, String pinyin, String meaning,
                         int level, String topic, String ex, String exMeaning) {
        return Vocabulary.builder()
            .hanzi(hanzi).pinyin(pinyin).meaningVn(meaning)
            .hskLevel(level).topic(topic)
            .exampleSentence(ex).exampleMeaning(exMeaning)
            .build();
    }
}
