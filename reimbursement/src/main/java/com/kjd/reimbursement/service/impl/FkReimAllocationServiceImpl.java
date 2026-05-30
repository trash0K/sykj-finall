package com.kjd.reimbursement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kjd.reimbursement.mapper.FkReimAllocationMapper;
import com.kjd.reimbursement.pojo.entity.FkReimAllocation;
import com.kjd.reimbursement.service.FkReimAllocationService;
import org.springframework.stereotype.Service;

@Service
public class FkReimAllocationServiceImpl extends ServiceImpl<FkReimAllocationMapper, FkReimAllocation> implements FkReimAllocationService {
}
