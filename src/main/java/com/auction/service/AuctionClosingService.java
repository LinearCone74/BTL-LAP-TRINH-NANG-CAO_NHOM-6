package com.auction.service;

import com.auction.model.auction.AuctionView;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class AuctionClosingService {

    public boolean checkAndClose(AuctionView auctionView) {
        if (auctionView == null) {
            return false;
        }

        LocalDateTime endTime = getEndTime(auctionView);

        if (endTime == null) {
            return false;
        }

        if (LocalDateTime.now().isBefore(endTime)) {
            return false;
        }

        String currentStatus = getStatusText(auctionView);

        if ("FINISHED".equals(currentStatus)
                || "PAID".equals(currentStatus)
                || "CANCELED".equals(currentStatus)) {
            return false;
        }

        return setStatusToFinished(auctionView);
    }

    public String getWinnerName(AuctionView auctionView) {
        if (auctionView == null) {
            return "Không có người thắng";
        }

        try {
            Method getHighestBidderMethod = auctionView.getClass().getMethod("getHighestBidder");
            Object bidder = getHighestBidderMethod.invoke(auctionView);

            if (bidder == null) {
                return "Không có người thắng";
            }

            Method getFullNameMethod = bidder.getClass().getMethod("getFullName");
            Object fullName = getFullNameMethod.invoke(bidder);

            if (fullName == null || fullName.toString().isBlank()) {
                return "Không có người thắng";
            }

            return fullName.toString();

        } catch (Exception exception) {
            return "Không có người thắng";
        }
    }

    private LocalDateTime getEndTime(AuctionView auctionView) {
        try {
            Method method = auctionView.getClass().getMethod("getEndTime");
            Object value = method.invoke(auctionView);

            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }

            return null;

        } catch (Exception exception) {
            return null;
        }
    }

    private String getStatusText(AuctionView auctionView) {
        try {
            Method method = auctionView.getClass().getMethod("getStatus");
            Object value = method.invoke(auctionView);

            if (value == null) {
                return "";
            }

            return value.toString();

        } catch (Exception exception) {
            return "";
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean setStatusToFinished(AuctionView auctionView) {
        try {
            Method getStatusMethod = auctionView.getClass().getMethod("getStatus");
            Object currentStatus = getStatusMethod.invoke(auctionView);

            if (currentStatus == null) {
                return false;
            }

            Class<?> statusClass = currentStatus.getClass();
            Object finishedStatus = Enum.valueOf((Class<Enum>) statusClass, "FINISHED");

            Method setStatusMethod = auctionView.getClass().getMethod("setStatus", statusClass);
            setStatusMethod.invoke(auctionView, finishedStatus);

            return true;

        } catch (Exception exception) {
            System.out.println("Không thể chuyển trạng thái phiên đấu giá sang FINISHED: "
                    + exception.getMessage());
            return false;
        }
    }
}
