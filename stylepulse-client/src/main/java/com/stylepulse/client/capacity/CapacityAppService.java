package com.stylepulse.client.capacity;

import com.stylepulse.client.capacity.req.ConfirmCapacityRequestDTO;
import com.stylepulse.client.capacity.req.ReserveCapacityRequestDTO;
import com.stylepulse.client.capacity.res.ConfirmCapacityResponseDTO;
import com.stylepulse.client.capacity.res.ReserveCapacityResponseDTO;
import com.stylepulse.client.common.ApplicationCmdService;
import com.stylepulse.common.result.ResultDO;

/**
 * 产能应用服务（写模式）。
 *
 * <p>对外暴露产能操作的意义：运营可以在返单之外单独锁产能（比如提前给双十一备货），
 * 也可以让其他系统复用同一套产能账本。</p>
 */
public interface CapacityAppService extends ApplicationCmdService {

    ResultDO<ReserveCapacityResponseDTO> reserveCapacity(ReserveCapacityRequestDTO requestDTO);

    ResultDO<ConfirmCapacityResponseDTO> confirmCapacity(ConfirmCapacityRequestDTO requestDTO);
}