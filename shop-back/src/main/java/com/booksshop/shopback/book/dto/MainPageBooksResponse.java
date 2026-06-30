package com.booksshop.shopback.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MainPageBooksResponse {

    private final List<BookSummaryDto> bestTopN;
    private final List<BookSummaryDto> newTopN;
    private final List<BookSummaryDto> ItTopN;
    private final List<BookSummaryDto> novelTopN;
    private final List<BookSummaryDto> selfTopN;

    public MainPageBooksResponse(
            List<BookSummaryDto> bestTopN,
            List<BookSummaryDto> newTopN,
            List<BookSummaryDto> ItTopN,
            List<BookSummaryDto> novelTopN,
            List<BookSummaryDto> selfTopN
    ) {
        this.bestTopN = bestTopN;
        this.newTopN = newTopN;
        this.ItTopN = ItTopN;
        this.novelTopN = novelTopN;
        this.selfTopN = selfTopN;
    }

    public List<BookSummaryDto> getBestTopN() {
        return bestTopN;
    }

    public List<BookSummaryDto> getNewTopN() {
        return newTopN;
    }

    @JsonProperty("ItTopN")
    public List<BookSummaryDto> getItTopN() {
        return ItTopN;
    }

    public List<BookSummaryDto> getNovelTopN() {
        return novelTopN;
    }

    public List<BookSummaryDto> getSelfTopN() {
        return selfTopN;
    }
}
