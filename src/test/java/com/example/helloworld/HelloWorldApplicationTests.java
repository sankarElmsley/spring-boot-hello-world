package com.example.helloworld;

import com.example.helloworld.controller.HelloWorldController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
class HelloWorldApplicationTests {

    @Autowired
    private HelloWorldController helloWorldController;

    @Test
    void contextLoads() {
        // to ensure that controller is getting created inside the application context
        assertNotNull(helloWorldController);
    }

    

}



import { jest, describe, it, expect, beforeEach } from '@jest/globals';
import { CreditcardUtilityService } from '../../src/services/creditcardUtilityService';
import { CrossAccountAPICache } from '../../../common/cross-account/cached-apiinformation';
import { ServerError } from '../../util/serverError';
import { ErrorCodes } from '../../util/errorCodes';

// Mock lodash
jest.mock('lodash', () => ({
    cloneDeepWith: jest.fn(),
    isObject: jest.fn(),
    toLower: jest.fn(),
    some: jest.fn()
}));

// Mock uuid
jest.mock('uuid', () => ({
    v4: jest.fn()
}));

// Mock logger
jest.mock('../../../common/utils/logger', () => ({
    error: jest.fn(),
    trace: jest.fn(),
    info: jest.fn(),
    logBackendServiceRequest: jest.fn(),
    logBackendServiceResponse: jest.fn(),
    logBackendServiceError: jest.fn()
}));

// Mock binRangesResponse
jest.mock('../../model/ccu/binRangesResponse', () => ({
    logBackendServiceRequest: jest.fn(),
    logBackendServiceResponse: jest.fn(),
    logBackendServiceError: jest.fn(),
    error: jest.fn(),
    trace: jest.fn(),
    info: jest.fn()
}));

// Mock CrossAccountAPICache
jest.mock('../../../common/cross-account/cached-apiinformation', () => ({
    CrossAccountAPICache: jest.fn().mockImplementation(() => ({
        Value: () => Promise.resolve({
            expected: {
                apiKey: 'test-api-key',
                apiGatewayId: 'test-gateway-id'
            }
        })
    }))
}));

describe('CreditcardUtilityService', () => {
    const mockCorrelationId = 'test-correlation-id';

    beforeEach(() => {
        jest.clearAllMocks();
        // Set up environment variables
        process.env.crossRegion = 'test-region';
        process.env.REGION = 'test-region';
        process.env.crossAccountRoleForCCU = 'test-role';
        process.env.ssmParamCcuApiKey = 'test-param-key';
        process.env.ssmParamCcuGatewayId = 'test-param-gateway';
        process.env.getBinRangesUrl = '12345';

        // Mock global fetch
        global.fetch = jest.fn();
    });

    describe('getBinRanges', () => {
        it('should successfully retrieve bin ranges', () => {
            const mockBinRanges = {
                binList: {
                    bin: [{
                        binStart: '400000',
                        binEnd: '499999',
                        identifier: 'TEST-BIN-001'
                    }]
                }
            };

            (global.fetch as jest.Mock).mockImplementation(() => 
                Promise.resolve({
                    status: 200,
                    json: () => Promise.resolve(mockBinRanges)
                })
            );

            // Create a new instance
            const service = new CreditcardUtilityService(mockCorrelationId);

            return service.getBinRanges().then((result: any) => {
                expect(result).toEqual(mockBinRanges.binList.bin);
                expect(global.fetch).toHaveBeenCalledWith(
                    12345,
                    expect.objectContaining({
                        method: 'GET',
                        headers: expect.objectContaining({
                            'x-api-key': 'test-api-key',
                            'x-apigw-api-id': 'test-gateway-id',
                            'x-request-id': mockCorrelationId
                        })
                    })
                );
            });
        });

        it('should throw error for non-200 response', () => {
            (global.fetch as jest.Mock).mockImplementation(() =>
                Promise.resolve({
                    status: 500,
                    statusText: 'Internal Server Error'
                })
            );

            const service = new CreditcardUtilityService(mockCorrelationId);
            
            return service.getBinRanges().catch((error: ServerError) => {
                expect(error).toEqual(
                    new ServerError(
                        ErrorCodes.BIN_RANGES_API_INVALID_RESPONSE_STATUS.errorMessage,
                        ErrorCodes.BIN_RANGES_API_INVALID_RESPONSE_STATUS.errorCode
                    )
                );
            });
        });

        it('should throw error when API call fails', () => {
            (global.fetch as jest.Mock).mockImplementation(() =>
                Promise.reject(new Error('Network error'))
            );

            const service = new CreditcardUtilityService(mockCorrelationId);
            
            return service.getBinRanges().catch((error: ServerError) => {
                expect(error).toEqual(
                    new ServerError(
                        ErrorCodes.BIN_RANGES_API_UNEXPECTED_ERROR.errorMessage,
                        ErrorCodes.BIN_RANGES_API_UNEXPECTED_ERROR.errorCode
                    )
                );
            });
        });

        it('should throw error when cache initialization fails', () => {
            (CrossAccountAPICache as jest.Mock).mockImplementationOnce(() => {
                throw new Error('Cache initialization failed');
            });

            expect(() => new CreditcardUtilityService(mockCorrelationId)).toThrow(
                new ServerError(
                    ErrorCodes.BIN_RANGES_API_UNEXPECTED_ERROR_SSM_PARAM.errorMessage,
                    ErrorCodes.BIN_RANGES_API_UNEXPECTED_ERROR_SSM_PARAM.errorCode
                )
            );
        });
    });
});
