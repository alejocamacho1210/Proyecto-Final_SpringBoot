package com.coworkhub.business;

import java.time.YearMonth;
import java.util.List;
import com.coworkhub.domain.MemberStatus;
import com.coworkhub.presentation.ApiDtos.*;

public interface MemberService {
    List<MemberResponse> list(String name, MemberStatus status);
    MemberResponse get(Long id);
    MemberResponse create(MemberRequest request);
    MemberResponse update(Long id, MemberUpdateRequest request);
    MemberResponse setStatus(Long id, MemberStatus status);
    void delete(Long id);
    ConsumptionResponse consumption(Long id, YearMonth month);
}
