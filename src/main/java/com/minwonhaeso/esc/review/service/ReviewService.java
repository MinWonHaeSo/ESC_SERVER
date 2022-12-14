package com.minwonhaeso.esc.review.service;

import com.minwonhaeso.esc.error.exception.ReviewException;
import com.minwonhaeso.esc.error.exception.StadiumException;
import com.minwonhaeso.esc.member.model.entity.Member;
import com.minwonhaeso.esc.notification.service.NotificationService;
import com.minwonhaeso.esc.review.model.dto.ReviewDto;
import com.minwonhaeso.esc.review.model.entity.Review;
import com.minwonhaeso.esc.review.repository.ReviewRepository;
import com.minwonhaeso.esc.stadium.model.entity.Stadium;
import com.minwonhaeso.esc.stadium.model.type.StadiumReservationStatus;
import com.minwonhaeso.esc.stadium.repository.StadiumRepository;
import com.minwonhaeso.esc.stadium.repository.StadiumReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.minwonhaeso.esc.error.type.ReviewErrorCode.*;
import static com.minwonhaeso.esc.error.type.StadiumErrorCode.StadiumNotFound;
import static com.minwonhaeso.esc.notification.model.type.NotificationType.REVIEW;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final StadiumRepository stadiumRepository;
    private final StadiumReservationRepository stadiumReservationRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<ReviewDto.Response> getAllReviewsByStadium(Long stadiumId, Pageable pageable) {
        Stadium stadium = findByStadiumId(stadiumId);
        return reviewRepository.findAllByStadium(stadium, pageable).map(ReviewDto.Response::fromEntity);
    }

    @Transactional
    public ReviewDto.Response createReview(ReviewDto.Request request, Member member, Long stadiumId) {
        Stadium stadium = findByStadiumId(stadiumId);

        Long reservationCount = stadiumReservationRepository.countAllByMemberAndStadiumAndStatusIs(
                member, stadium, StadiumReservationStatus.EXECUTED);
        log.info("[Stadium - " + stadiumId + "] ?????? ????????? ??? " + reservationCount + "????????????.");

        Long reviewCount = reviewRepository.countAllByMemberAndStadium(member, stadium);
        log.info("[Stadium - " + stadiumId + "] ?????? ???, ???????????? ????????? ??? " + reviewCount + "????????????.");

        if(reservationCount == 0) {
            throw new ReviewException(NoExecutedReservationForReview);
        } else if (reservationCount <= reviewCount) {
            throw new ReviewException(ReviewCountOverReservation);
        }

        Review review = Review.builder()
                .star(request.getStar())
                .comment(request.getComment())
                .stadium(stadium)
                .member(member)
                .build();

        reviewRepository.save(review);
        log.info("?????? ?????? [ " + member.getMemberId() + " ] -  ????????? ?????????????????????.");

        notificationService.createNotification(REVIEW, stadiumId,
                "????????? [ " + stadium.getName() + " ]??? ????????? ????????? ?????????????????????.", stadium.getMember());
        log.info("?????? ?????? [ " + stadium.getMember().getMemberId() + " ] ??? ????????? ?????????????????????.");

        return ReviewDto.Response.fromEntity(review);
    }

    public void deleteReview(Member member, Long stadiumId, Long reviewId) {
        Stadium stadium = findByStadiumId(stadiumId);
        Review review = findByIdAndStadium(reviewId, stadium);

        if (!Objects.equals(review.getMember().getMemberId(), member.getMemberId())) {
            throw new ReviewException(UnAuthorizedAccess);
        }

        reviewRepository.delete(review);
        log.info("?????? ?????? [ " + member.getMemberId() + " ] - [ " + reviewId + " ] ????????? ?????????????????????.");
    }

    public ReviewDto.Response updateReview(ReviewDto.Request request, Member member, Long stadiumId, Long reviewId) {
        Stadium stadium = findByStadiumId(stadiumId);
        Review review = findByIdAndStadium(reviewId, stadium);

        if (!Objects.equals(review.getMember().getMemberId(), member.getMemberId())) {
            throw new ReviewException(UnAuthorizedAccess);
        }

        review.update(request.getStar(), request.getComment());
        reviewRepository.save(review);
        log.info("?????? ?????? [ " + member.getMemberId() + " ] - [ " + reviewId + " ] ????????? ?????????????????????.");

        return ReviewDto.Response.fromEntity(review);
    }

    @Scheduled(cron = "${scheduler.update.starAvg}")
    public void updateStarAvg() {
        List<Stadium> stadiums = stadiumRepository.findAll();
        for(Stadium stadium : stadiums) {
            Double starAvg = reviewRepository.findStarAvg(stadium);
            stadium.setStarAvg(starAvg);
            stadiumRepository.save(stadium);
        }
        log.info("[" + LocalDateTime.now() + "] ????????? ????????? ???????????? ???????????????.");
    }

    private Stadium findByStadiumId(Long stadiumId) {
        return stadiumRepository.findById(stadiumId).orElseThrow(() -> new StadiumException(StadiumNotFound));
    }

    private Review findByIdAndStadium(Long reviewId, Stadium stadium) {
        return reviewRepository.findByIdAndStadium(reviewId, stadium)
                .orElseThrow(() -> new ReviewException(ReviewNotFound));
    }

}